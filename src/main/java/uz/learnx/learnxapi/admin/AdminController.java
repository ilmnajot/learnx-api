package uz.learnx.learnxapi.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.common.security.CurrentUserId;
import uz.learnx.learnxapi.community.CommentMapper;
import uz.learnx.learnxapi.community.CommentService;
import uz.learnx.learnxapi.community.SiteSettings;

/** Every route here is guarded by {@link AdminAccess}. */
@RestController
@Validated
@RequestMapping("/api/v1/admin")
class AdminController {

    record RoleRequest(@NotNull Boolean admin) {}

    record VisibilityRequest(@NotNull Boolean hidden) {}

    record ModerationRequest(@NotNull CommentService.Moderation status) {}

    private final AdminService admin;
    private final CommentService comments;
    private final SiteSettings settings;

    AdminController(AdminService admin, CommentService comments, SiteSettings settings) {
        this.admin = admin;
        this.comments = comments;
        this.settings = settings;
    }

    @GetMapping("/stats")
    AdminService.Stats stats() {
        return admin.stats();
    }

    @GetMapping("/users")
    AdminService.UserPage users(@RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return admin.searchUsers(q, page, size);
    }

    @PostMapping("/users/{id}/ban")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void ban(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        admin.ban(adminId, id);
    }

    @PostMapping("/users/{id}/unban")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unban(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        admin.unban(adminId, id);
    }

    @PostMapping("/users/{id}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void role(@CurrentUserId UUID adminId, @PathVariable UUID id, @Valid @RequestBody RoleRequest request) {
        admin.setAdmin(adminId, id, request.admin());
    }

    @PostMapping("/users/{id}/confirm-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void confirmEmail(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        admin.confirmEmail(adminId, id);
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        admin.delete(adminId, id);
    }

    @GetMapping("/comments")
    List<CommentMapper.AdminComment> comments(@RequestParam(required = false) CommentService.Status status) {
        return comments.forModeration(status);
    }

    @PostMapping("/comments/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void moderate(@CurrentUserId UUID adminId, @PathVariable UUID id, @Valid @RequestBody ModerationRequest request) {
        comments.moderate(id, request.status());
        admin.record(adminId, "COMMENT_" + request.status(), id.toString());
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteComment(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        comments.delete(id);
        admin.record(adminId, "COMMENT_DELETED", id.toString());
    }

    @GetMapping("/videos")
    List<AdminMapper.VideoRow> videos() {
        return admin.videos();
    }

    @PostMapping("/videos/{id}/visibility")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void visibility(@CurrentUserId UUID adminId, @PathVariable UUID id, @Valid @RequestBody VisibilityRequest request) {
        admin.setVideoHidden(adminId, id, request.hidden());
    }

    @GetMapping("/settings")
    SiteSettings.Values settings() {
        return settings.load();
    }

    @PutMapping("/settings")
    SiteSettings.Values updateSettings(@CurrentUserId UUID adminId, @Valid @RequestBody SiteSettings.Values values) {
        SiteSettings.Values saved = settings.save(values);
        // The audit entry deliberately leaves out the values, so card details never spread into the log.
        admin.record(adminId, "SETTINGS_UPDATED", null);
        return saved;
    }

    @GetMapping("/audit")
    List<AdminMapper.AuditRow> audit(@RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return admin.auditLog(limit);
    }
}
