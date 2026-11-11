package uz.learnx.learnxapi.service;

import java.util.List;
import java.util.UUID;
import uz.learnx.learnxapi.dto.CommentDto;
import uz.learnx.learnxapi.model.ApiResponse;

/** Reviews go through moderation before they appear publicly, so spam and abuse never reach the homepage. */
public interface CommentService {
    ApiResponse mine(UUID userId);

    ApiResponse save(UUID userId, CommentDto.CommentRequest request);

    ApiResponse deleteMine(UUID userId);

    List<CommentDto.AdminComment> forModeration(CommentDto.Status status);

    void moderate(UUID commentId, CommentDto.Moderation status);

    void delete(UUID commentId);
}
