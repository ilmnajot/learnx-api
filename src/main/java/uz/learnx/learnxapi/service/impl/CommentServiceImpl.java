package uz.learnx.learnxapi.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.learnx.learnxapi.dto.CommentDto;
import uz.learnx.learnxapi.dto.User;
import uz.learnx.learnxapi.exception.ApiException;
import uz.learnx.learnxapi.mapper.CommentMapper;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.service.CommentService;
import uz.learnx.learnxapi.service.SiteSettingsService;
import uz.learnx.learnxapi.service.UserService;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final UserService userService;
    private final SiteSettingsService siteSettingsService;
    private final Clock clock;

    /** No data means the learner hasn't written a review yet. */
    @Override
    public ApiResponse mine(UUID userId) {
        return ApiResponse.ok(this.commentMapper.own(userId).orElse(null));
    }

    /** Requires a confirmed email, so throwaway accounts can't fill the moderation queue. */
    @Override
    public ApiResponse save(UUID userId, CommentDto.CommentRequest request) {
        User user = this.userService.getUser(userId);
        if (user.emailVerifiedAt() == null) {
            throw ApiException.forbidden("email_not_verified", "Confirm your email to leave a review");
        }
        if (user.bannedAt() != null) {
            throw ApiException.forbidden("account_banned", "This account has been blocked");
        }
        if (!this.siteSettingsService.load().commentsEnabled()) {
            throw ApiException.conflict("comments_disabled", "Reviews are closed at the moment");
        }
        this.commentMapper.upsert(userId, request.body().strip(), request.rating());
        return ApiResponse.ok(this.commentMapper.own(userId).orElseThrow());
    }

    @Override
    public ApiResponse deleteMine(UUID userId) {
        this.commentMapper.deleteOwn(userId);
        return ApiResponse.ok();
    }

    @Override
    public List<CommentDto.AdminComment> forModeration(CommentDto.Status status) {
        return this.commentMapper.forModeration(status == null ? null : status.name());
    }

    @Override
    public void moderate(UUID commentId, CommentDto.Moderation status) {
        if (this.commentMapper.setStatus(commentId, status.name(), Instant.now(this.clock)) == 0) {
            throw ApiException.notFound("Comment");
        }
    }

    @Override
    public void delete(UUID commentId) {
        if (this.commentMapper.delete(commentId) == 0) {
            throw ApiException.notFound("Comment");
        }
    }
}
