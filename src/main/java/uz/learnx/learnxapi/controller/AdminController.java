package uz.learnx.learnxapi.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.dto.AdminDto;
import uz.learnx.learnxapi.dto.CommentDto;
import uz.learnx.learnxapi.dto.SiteDto;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.security.AdminAccessInterceptor;
import uz.learnx.learnxapi.security.CurrentUserId;
import uz.learnx.learnxapi.service.AdminService;

/** Every route here is guarded by {@link AdminAccessInterceptor}. */
@RestController
@Validated
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ApiResponse stats() {
        return this.adminService.stats();
    }

    @GetMapping("/users")
    public ApiResponse users(@RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return this.adminService.searchUsers(q, page, size);
    }

    @PostMapping("/users/{id}/ban")
    public ApiResponse ban(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        return this.adminService.ban(adminId, id);
    }

    @PostMapping("/users/{id}/unban")
    public ApiResponse unban(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        return this.adminService.unban(adminId, id);
    }

    @PostMapping("/users/{id}/role")
    public ApiResponse role(@CurrentUserId UUID adminId, @PathVariable UUID id,
            @Valid @RequestBody AdminDto.RoleRequest request) {
        return this.adminService.setAdmin(adminId, id, request);
    }

    @PostMapping("/users/{id}/confirm-email")
    public ApiResponse confirmEmail(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        return this.adminService.confirmEmail(adminId, id);
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse delete(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        return this.adminService.deleteUser(adminId, id);
    }

    @GetMapping("/comments")
    public ApiResponse comments(@RequestParam(required = false) CommentDto.Status status) {
        return this.adminService.comments(status);
    }

    @PostMapping("/comments/{id}/status")
    public ApiResponse moderate(@CurrentUserId UUID adminId, @PathVariable UUID id,
            @Valid @RequestBody AdminDto.ModerationRequest request) {
        return this.adminService.moderateComment(adminId, id, request);
    }

    @DeleteMapping("/comments/{id}")
    public ApiResponse deleteComment(@CurrentUserId UUID adminId, @PathVariable UUID id) {
        return this.adminService.deleteComment(adminId, id);
    }

    @GetMapping("/videos")
    public ApiResponse videos() {
        return this.adminService.videos();
    }

    @PostMapping("/videos/{id}/visibility")
    public ApiResponse visibility(@CurrentUserId UUID adminId, @PathVariable UUID id,
            @Valid @RequestBody AdminDto.VisibilityRequest request) {
        return this.adminService.setVideoHidden(adminId, id, request);
    }

    @GetMapping("/settings")
    public ApiResponse settings() {
        return this.adminService.settings();
    }

    @PutMapping("/settings")
    public ApiResponse updateSettings(@CurrentUserId UUID adminId, @Valid @RequestBody SiteDto.Settings settings) {
        return this.adminService.updateSettings(adminId, settings);
    }

    @GetMapping("/audit")
    public ApiResponse audit(@RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return this.adminService.auditLog(limit);
    }
}
