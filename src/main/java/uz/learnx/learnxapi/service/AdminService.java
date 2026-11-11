package uz.learnx.learnxapi.service;

import java.util.UUID;
import uz.learnx.learnxapi.dto.AdminDto;
import uz.learnx.learnxapi.dto.CommentDto;
import uz.learnx.learnxapi.dto.SiteDto;
import uz.learnx.learnxapi.model.ApiResponse;

public interface AdminService {
    ApiResponse stats();

    ApiResponse searchUsers(String query, int page, int size);

    ApiResponse ban(UUID adminId, UUID userId);

    ApiResponse unban(UUID adminId, UUID userId);

    ApiResponse setAdmin(UUID adminId, UUID userId, AdminDto.RoleRequest request);

    ApiResponse confirmEmail(UUID adminId, UUID userId);

    ApiResponse deleteUser(UUID adminId, UUID userId);

    ApiResponse comments(CommentDto.Status status);

    ApiResponse moderateComment(UUID adminId, UUID commentId, AdminDto.ModerationRequest request);

    ApiResponse deleteComment(UUID adminId, UUID commentId);

    ApiResponse videos();

    ApiResponse setVideoHidden(UUID adminId, UUID videoId, AdminDto.VisibilityRequest request);

    ApiResponse settings();

    ApiResponse updateSettings(UUID adminId, SiteDto.Settings settings);

    ApiResponse auditLog(int limit);
}
