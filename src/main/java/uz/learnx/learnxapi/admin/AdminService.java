package uz.learnx.learnxapi.admin;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.learnx.learnxapi.admin.AdminMapper.AuditRow;
import uz.learnx.learnxapi.admin.AdminMapper.DayCount;
import uz.learnx.learnxapi.admin.AdminMapper.LevelCount;
import uz.learnx.learnxapi.admin.AdminMapper.Totals;
import uz.learnx.learnxapi.admin.AdminMapper.UserRow;
import uz.learnx.learnxapi.admin.AdminMapper.VideoRow;
import uz.learnx.learnxapi.auth.SessionRevoker;
import uz.learnx.learnxapi.common.config.LearnxProperties;
import uz.learnx.learnxapi.common.error.ApiException;
import uz.learnx.learnxapi.squad.SquadMapper;
import uz.learnx.learnxapi.squad.SquadService;
import uz.learnx.learnxapi.user.UserMapper;

@Service
public class AdminService {

    public record Stats(Totals totals, int aiGlobalDailyLimit, List<LevelCount> levels, List<DayCount> signups) {}

    public record UserPage(List<UserRow> users, int total, int page, int size) {}

    private static final int SIGNUP_CHART_DAYS = 30;

    private final AdminMapper admin;
    private final UserMapper users;
    private final SessionRevoker sessions;
    private final SquadService squads;
    private final SquadMapper squadMapper;
    private final AdminBootstrap bootstrap;
    private final LearnxProperties properties;
    private final Clock clock;

    AdminService(AdminMapper admin, UserMapper users, SessionRevoker sessions, SquadService squads,
            SquadMapper squadMapper, AdminBootstrap bootstrap, LearnxProperties properties, Clock clock) {
        this.admin = admin;
        this.users = users;
        this.sessions = sessions;
        this.squads = squads;
        this.squadMapper = squadMapper;
        this.bootstrap = bootstrap;
        this.properties = properties;
        this.clock = clock;
    }

    public Stats stats() {
        Instant now = Instant.now(clock);
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        return new Stats(
                admin.totals(today, today.minusDays(6), now.minus(Duration.ofDays(7))),
                properties.ai().globalDailyLimit(),
                admin.levels(),
                admin.signupsByDay(now.minus(Duration.ofDays(SIGNUP_CHART_DAYS))));
    }

    public UserPage searchUsers(String query, int page, int size) {
        String search = query == null || query.isBlank() ? null : "%" + escapeLike(query.strip()) + "%";
        return new UserPage(admin.users(search, size, page * size), admin.countUsers(search), page, size);
    }

    /** A search for "100%" must look for a percent sign, not treat it as a wildcard. */
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Transactional
    public void ban(UUID adminId, UUID userId) {
        requireOtherUser(adminId, userId);
        requireFound(admin.setBanned(userId, Instant.now(clock)));
        sessions.revokeAll(userId);
        record(adminId, "USER_BANNED", target(userId));
    }

    @Transactional
    public void unban(UUID adminId, UUID userId) {
        requireFound(admin.setBanned(userId, null));
        record(adminId, "USER_UNBANNED", target(userId));
    }

    @Transactional
    public void setAdmin(UUID adminId, UUID userId, boolean makeAdmin) {
        requireOtherUser(adminId, userId);
        String email = target(userId);
        if (!makeAdmin && bootstrap.isConfigured(email)) {
            throw ApiException.conflict("admin_configured", "This admin is set in LEARNX_ADMIN_EMAILS on the server");
        }
        requireFound(admin.setRole(userId, makeAdmin ? "ADMIN" : "USER"));
        record(adminId, makeAdmin ? "ADMIN_GRANTED" : "ADMIN_REMOVED", email);
    }

    @Transactional
    public void confirmEmail(UUID adminId, UUID userId) {
        String email = target(userId);
        users.markVerified(userId, Instant.now(clock));
        bootstrap.promoteConfigured();
        record(adminId, "EMAIL_CONFIRMED", email);
    }

    @Transactional
    public void delete(UUID adminId, UUID userId) {
        requireOtherUser(adminId, userId);
        String email = target(userId);
        if (squadMapper.findByMember(userId).isPresent()) {
            squads.leave(userId);
        }
        requireFound(admin.deleteUser(userId));
        record(adminId, "USER_DELETED", email);
    }

    public List<VideoRow> videos() {
        return admin.videos();
    }

    @Transactional
    public void setVideoHidden(UUID adminId, UUID videoId, boolean hidden) {
        if (admin.setVideoHidden(videoId, hidden ? Instant.now(clock) : null) == 0) {
            throw ApiException.notFound("Video");
        }
        record(adminId, hidden ? "VIDEO_HIDDEN" : "VIDEO_SHOWN", videoId.toString());
    }

    public List<AuditRow> auditLog(int limit) {
        return admin.auditLog(limit);
    }

    /** Records an admin action, in the same transaction as the action itself. */
    public void record(UUID adminId, String action, String target) {
        admin.audit(adminId, admin.emailOf(adminId).orElse("unknown"), action, target);
    }

    private String target(UUID userId) {
        return admin.emailOf(userId).orElseThrow(() -> ApiException.notFound("User"));
    }

    private static void requireFound(int updated) {
        if (updated == 0) {
            throw ApiException.notFound("User");
        }
    }

    /** Stops an admin locking themselves out, which also guarantees at least one admin always remains. */
    private static void requireOtherUser(UUID adminId, UUID userId) {
        if (adminId.equals(userId)) {
            throw ApiException.conflict("cannot_change_self", "You can't do this to your own account");
        }
    }
}
