package uz.learnx.learnxapi.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.learnx.learnxapi.config.LearnxProperties;
import uz.learnx.learnxapi.dto.AdminDto;
import uz.learnx.learnxapi.dto.CommentDto;
import uz.learnx.learnxapi.dto.SiteDto;
import uz.learnx.learnxapi.exception.ApiException;
import uz.learnx.learnxapi.mapper.AdminMapper;
import uz.learnx.learnxapi.mapper.UserMapper;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.service.AdminPromotionService;
import uz.learnx.learnxapi.service.AdminService;
import uz.learnx.learnxapi.service.AuthService;
import uz.learnx.learnxapi.service.CommentService;
import uz.learnx.learnxapi.service.SiteSettingsService;
import uz.learnx.learnxapi.service.SquadService;

/** Every admin action is written to the audit log, in the same transaction as the action itself. */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final int SIGNUP_CHART_DAYS = 30;

    private final AdminMapper adminMapper;
    private final UserMapper userMapper;
    private final AuthService authService;
    private final SquadService squadService;
    private final CommentService commentService;
    private final SiteSettingsService siteSettingsService;
    private final AdminPromotionService adminPromotionService;
    private final LearnxProperties properties;
    private final Clock clock;

    @Override
    public ApiResponse stats() {
        Instant now = Instant.now(this.clock);
        LocalDate today = LocalDate.now(this.clock.withZone(ZoneOffset.UTC));
        return ApiResponse.ok(new AdminDto.Stats(
                this.adminMapper.totals(today, today.minusDays(6), now.minus(Duration.ofDays(7))),
                this.properties.ai().globalDailyLimit(),
                this.adminMapper.levels(),
                this.adminMapper.signupsByDay(now.minus(Duration.ofDays(SIGNUP_CHART_DAYS)))));
    }

    @Override
    public ApiResponse searchUsers(String query, int page, int size) {
        String search = query == null || query.isBlank() ? null : "%" + escapeLike(query.strip()) + "%";
        return ApiResponse.ok(new AdminDto.UserPage(this.adminMapper.users(search, size, page * size),
                this.adminMapper.countUsers(search), page, size));
    }

    /** A search for "100%" must look for a percent sign, not treat it as a wildcard. */
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Override
    @Transactional
    public ApiResponse ban(UUID adminId, UUID userId) {
        requireOtherUser(adminId, userId);
        requireFound(this.adminMapper.setBanned(userId, Instant.now(this.clock)));
        this.authService.revokeAllSessions(userId);
        record(adminId, "USER_BANNED", target(userId));
        return ApiResponse.ok();
    }

    @Override
    @Transactional
    public ApiResponse unban(UUID adminId, UUID userId) {
        requireFound(this.adminMapper.setBanned(userId, null));
        record(adminId, "USER_UNBANNED", target(userId));
        return ApiResponse.ok();
    }

    @Override
    @Transactional
    public ApiResponse setAdmin(UUID adminId, UUID userId, AdminDto.RoleRequest request) {
        requireOtherUser(adminId, userId);
        boolean makeAdmin = request.admin();
        String email = target(userId);
        if (!makeAdmin && this.adminPromotionService.isConfigured(email)) {
            throw ApiException.conflict("admin_configured", "This admin is set in LEARNX_ADMIN_EMAILS on the server");
        }
        requireFound(this.adminMapper.setRole(userId, makeAdmin ? "ADMIN" : "USER"));
        record(adminId, makeAdmin ? "ADMIN_GRANTED" : "ADMIN_REMOVED", email);
        return ApiResponse.ok();
    }

    @Override
    @Transactional
    public ApiResponse confirmEmail(UUID adminId, UUID userId) {
        String email = target(userId);
        this.userMapper.markVerified(userId, Instant.now(this.clock));
        this.adminPromotionService.promoteConfigured();
        record(adminId, "EMAIL_CONFIRMED", email);
        return ApiResponse.ok();
    }

    @Override
    @Transactional
    public ApiResponse deleteUser(UUID adminId, UUID userId) {
        requireOtherUser(adminId, userId);
        String email = target(userId);
        this.squadService.leaveIfMember(userId);
        requireFound(this.adminMapper.deleteUser(userId));
        record(adminId, "USER_DELETED", email);
        return ApiResponse.ok();
    }

    @Override
    public ApiResponse comments(CommentDto.Status status) {
        return ApiResponse.ok(this.commentService.forModeration(status));
    }

    @Override
    @Transactional
    public ApiResponse moderateComment(UUID adminId, UUID commentId, AdminDto.ModerationRequest request) {
        this.commentService.moderate(commentId, request.status());
        record(adminId, "COMMENT_" + request.status(), commentId.toString());
        return ApiResponse.ok();
    }

    @Override
    @Transactional
    public ApiResponse deleteComment(UUID adminId, UUID commentId) {
        this.commentService.delete(commentId);
        record(adminId, "COMMENT_DELETED", commentId.toString());
        return ApiResponse.ok();
    }

    @Override
    public ApiResponse videos() {
        return ApiResponse.ok(this.adminMapper.videos());
    }

    @Override
    @Transactional
    public ApiResponse setVideoHidden(UUID adminId, UUID videoId, AdminDto.VisibilityRequest request) {
        boolean hidden = request.hidden();
        if (this.adminMapper.setVideoHidden(videoId, hidden ? Instant.now(this.clock) : null) == 0) {
            throw ApiException.notFound("Video");
        }
        record(adminId, hidden ? "VIDEO_HIDDEN" : "VIDEO_SHOWN", videoId.toString());
        return ApiResponse.ok();
    }

    @Override
    public ApiResponse settings() {
        return ApiResponse.ok(this.siteSettingsService.load());
    }

    @Override
    @Transactional
    public ApiResponse updateSettings(UUID adminId, SiteDto.Settings settings) {
        SiteDto.Settings saved = this.siteSettingsService.save(settings);
        // The audit entry deliberately leaves out the values, so card details never spread into the log.
        record(adminId, "SETTINGS_UPDATED", null);
        return ApiResponse.ok(saved);
    }

    @Override
    public ApiResponse auditLog(int limit) {
        return ApiResponse.ok(this.adminMapper.auditLog(limit));
    }

    private void record(UUID adminId, String action, String target) {
        this.adminMapper.audit(adminId, this.adminMapper.emailOf(adminId).orElse("unknown"), action, target);
    }

    private String target(UUID userId) {
        return this.adminMapper.emailOf(userId).orElseThrow(() -> ApiException.notFound("User"));
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
