package uz.learnx.learnxapi.service.impl;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.learnx.learnxapi.dto.SquadDto;
import uz.learnx.learnxapi.event.PracticeRecorded;
import uz.learnx.learnxapi.exception.ApiException;
import uz.learnx.learnxapi.mapper.SquadMapper;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.service.SquadService;
import uz.learnx.learnxapi.service.UserService;
import uz.learnx.learnxapi.utils.DateUtils;
import uz.learnx.learnxapi.utils.SquadRules;
import uz.learnx.learnxapi.utils.StreakCalculator;

@Service
@RequiredArgsConstructor
public class SquadServiceImpl implements SquadService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int STREAK_LOOKBACK_DAYS = 365;

    private final SquadMapper squadMapper;
    private final UserService userService;
    private final SecureRandom random = new SecureRandom();

    /** No data means the learner has no squad yet, which is an ordinary state rather than an error. */
    @Override
    @Transactional
    public ApiResponse mine(UUID userId) {
        return ApiResponse.ok(view(userId).orElse(null));
    }

    @Override
    @Transactional
    public ApiResponse create(UUID userId, SquadDto.CreateRequest request) {
        requireNoSquad(userId);
        UUID id = UUID.randomUUID();
        try {
            this.squadMapper.insertSquad(id, request.name().strip(), newInviteCode(), userId);
            this.squadMapper.addMember(id, userId);
        } catch (DuplicateKeyException e) {
            throw alreadyInSquad();
        }
        return ApiResponse.created(view(userId).orElseThrow());
    }

    @Override
    @Transactional
    public ApiResponse join(UUID userId, SquadDto.JoinRequest request) {
        requireNoSquad(userId);
        SquadDto.Squad squad = this.squadMapper
                .findByInviteCodeForUpdate(request.inviteCode().strip().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> ApiException.notFound("Squad"));
        if (this.squadMapper.memberCount(squad.id()) >= SquadRules.MAX_MEMBERS) {
            throw ApiException.conflict("squad_full", "This squad is full");
        }
        try {
            this.squadMapper.addMember(squad.id(), userId);
        } catch (DuplicateKeyException e) {
            throw alreadyInSquad();
        }
        return ApiResponse.ok(view(userId).orElseThrow());
    }

    @Override
    @Transactional
    public ApiResponse leave(UUID userId) {
        SquadDto.Squad squad = this.squadMapper.findByMember(userId).orElseThrow(() -> ApiException.notFound("Squad"));
        leave(userId, squad);
        return ApiResponse.ok();
    }

    @Override
    @Transactional
    public void leaveIfMember(UUID userId) {
        this.squadMapper.findByMember(userId).ifPresent(squad -> leave(userId, squad));
    }

    /** Every point of practice this week helps resolve the squad's incident. */
    @EventListener
    public void on(PracticeRecorded event) {
        if (event.points() <= 0) {
            return;
        }
        this.squadMapper.findByMember(event.userId()).ifPresent(squad -> {
            SquadDto.Incident incident = currentIncident(squad.id(), this.userService.today(event.userId()));
            this.squadMapper.addIncidentPoints(incident.id(), event.points());
            this.squadMapper.addContribution(incident.id(), event.userId(), event.points());
        });
    }

    private void leave(UUID userId, SquadDto.Squad squad) {
        this.squadMapper.lock(squad.id());
        this.squadMapper.removeMember(squad.id(), userId);
        if (this.squadMapper.memberCount(squad.id()) == 0) {
            this.squadMapper.deleteSquad(squad.id());
        } else if (userId.equals(squad.ownerId())) {
            this.squadMapper.transferOwnership(squad.id());
        }
    }

    private Optional<SquadDto.SquadView> view(UUID userId) {
        return this.squadMapper.findByMember(userId).map(squad -> {
            LocalDate today = this.userService.today(userId);
            SquadDto.Incident incident = currentIncident(squad.id(), today);
            List<SquadDto.MemberRow> members = this.squadMapper.members(squad.id(), incident.id(), today);
            int required = SquadRules.requiredToday(members.size());
            int practised = (int) members.stream().filter(SquadDto.MemberRow::practisedToday).count();
            List<LocalDate> days =
                    this.squadMapper.qualifyingDays(squad.id(), today.minusDays(STREAK_LOOKBACK_DAYS), required);
            int target = SquadRules.incidentTarget(members.size());
            SquadDto.IncidentView incidentView = new SquadDto.IncidentView(incident.title(), incident.points(), target,
                    SquadRules.progressPercent(incident.points(), target), incident.points() >= target);
            return new SquadDto.SquadView(squad.id(), squad.name(), squad.inviteCode(), userId.equals(squad.ownerId()),
                    StreakCalculator.current(days, today), practised, required, practised >= required, incidentView,
                    members);
        });
    }

    private SquadDto.Incident currentIncident(UUID squadId, LocalDate today) {
        LocalDate weekStart = DateUtils.weekStart(today);
        this.squadMapper.ensureIncident(squadId, weekStart, SquadRules.incidentTitle(weekStart));
        return this.squadMapper.findIncident(squadId, weekStart);
    }

    private void requireNoSquad(UUID userId) {
        if (this.squadMapper.findByMember(userId).isPresent()) {
            throw alreadyInSquad();
        }
    }

    private static ApiException alreadyInSquad() {
        return ApiException.conflict("already_in_squad", "Leave your current squad first");
    }

    private String newInviteCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_ALPHABET.charAt(this.random.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }
}
