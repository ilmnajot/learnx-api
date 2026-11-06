package uz.learnx.learnxapi.squad;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.learnx.learnxapi.activity.PracticeRecorded;
import uz.learnx.learnxapi.activity.StreakCalculator;
import uz.learnx.learnxapi.common.error.ApiException;
import uz.learnx.learnxapi.squad.SquadMapper.Incident;
import uz.learnx.learnxapi.squad.SquadMapper.MemberRow;
import uz.learnx.learnxapi.squad.SquadMapper.Squad;
import uz.learnx.learnxapi.user.UserClock;

@Service
public class SquadService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int STREAK_LOOKBACK_DAYS = 365;

    public record IncidentView(String title, int points, int target, int progressPercent, boolean resolved) {}

    public record SquadView(UUID id, String name, String inviteCode, boolean owner, int streak, int practisedToday,
            int requiredToday, boolean streakSafeToday, IncidentView incident, List<MemberRow> members) {}

    private final SquadMapper squads;
    private final UserClock userClock;
    private final SecureRandom random = new SecureRandom();

    SquadService(SquadMapper squads, UserClock userClock) {
        this.squads = squads;
        this.userClock = userClock;
    }

    @Transactional
    public SquadView create(UUID userId, String name) {
        requireNoSquad(userId);
        UUID id = UUID.randomUUID();
        try {
            squads.insertSquad(id, name.strip(), newInviteCode(), userId);
            squads.addMember(id, userId);
        } catch (DuplicateKeyException e) {
            throw alreadyInSquad();
        }
        return view(userId).orElseThrow();
    }

    @Transactional
    public SquadView join(UUID userId, String inviteCode) {
        requireNoSquad(userId);
        Squad squad = squads.findByInviteCodeForUpdate(inviteCode.strip().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> ApiException.notFound("Squad"));
        if (squads.memberCount(squad.id()) >= SquadRules.MAX_MEMBERS) {
            throw ApiException.conflict("squad_full", "This squad is full");
        }
        try {
            squads.addMember(squad.id(), userId);
        } catch (DuplicateKeyException e) {
            throw alreadyInSquad();
        }
        return view(userId).orElseThrow();
    }

    @Transactional
    public void leave(UUID userId) {
        Squad squad = squads.findByMember(userId).orElseThrow(() -> ApiException.notFound("Squad"));
        squads.lock(squad.id());
        squads.removeMember(squad.id(), userId);
        if (squads.memberCount(squad.id()) == 0) {
            squads.deleteSquad(squad.id());
        } else if (userId.equals(squad.ownerId())) {
            squads.transferOwnership(squad.id());
        }
    }

    @Transactional
    public java.util.Optional<SquadView> view(UUID userId) {
        return squads.findByMember(userId).map(squad -> {
            LocalDate today = userClock.today(userId);
            Incident incident = currentIncident(squad.id(), today);
            List<MemberRow> members = squads.members(squad.id(), incident.id(), today);
            int required = SquadRules.requiredToday(members.size());
            int practised = (int) members.stream().filter(MemberRow::practisedToday).count();
            List<LocalDate> days =
                    squads.qualifyingDays(squad.id(), today.minusDays(STREAK_LOOKBACK_DAYS), required);
            int target = SquadRules.incidentTarget(members.size());
            IncidentView incidentView = new IncidentView(incident.title(), incident.points(), target,
                    SquadRules.progressPercent(incident.points(), target), incident.points() >= target);
            return new SquadView(squad.id(), squad.name(), squad.inviteCode(), userId.equals(squad.ownerId()),
                    StreakCalculator.current(days, today), practised, required, practised >= required, incidentView,
                    members);
        });
    }

    /** Every point of practice this week helps resolve the squad's incident. */
    @EventListener
    void on(PracticeRecorded event) {
        if (event.points() <= 0) {
            return;
        }
        squads.findByMember(event.userId()).ifPresent(squad -> {
            Incident incident = currentIncident(squad.id(), userClock.today(event.userId()));
            squads.addIncidentPoints(incident.id(), event.points());
            squads.addContribution(incident.id(), event.userId(), event.points());
        });
    }

    private Incident currentIncident(UUID squadId, LocalDate today) {
        LocalDate weekStart = UserClock.weekStart(today);
        squads.ensureIncident(squadId, weekStart, SquadRules.incidentTitle(weekStart));
        return squads.findIncident(squadId, weekStart);
    }

    private void requireNoSquad(UUID userId) {
        if (squads.findByMember(userId).isPresent()) {
            throw alreadyInSquad();
        }
    }

    private static ApiException alreadyInSquad() {
        return ApiException.conflict("already_in_squad", "Leave your current squad first");
    }

    private String newInviteCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }
}
