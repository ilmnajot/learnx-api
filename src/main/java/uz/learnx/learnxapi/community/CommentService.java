package uz.learnx.learnxapi.community;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.learnx.learnxapi.common.error.ApiException;
import uz.learnx.learnxapi.community.CommentMapper.AdminComment;
import uz.learnx.learnxapi.community.CommentMapper.OwnComment;
import uz.learnx.learnxapi.user.User;
import uz.learnx.learnxapi.user.UserService;

/** Reviews go through moderation before they appear publicly, so spam and abuse never reach the homepage. */
@Service
public class CommentService {

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }

    public enum Moderation {
        APPROVED,
        REJECTED
    }

    private final CommentMapper comments;
    private final UserService users;
    private final SiteSettings settings;
    private final Clock clock;

    CommentService(CommentMapper comments, UserService users, SiteSettings settings, Clock clock) {
        this.comments = comments;
        this.users = users;
        this.settings = settings;
        this.clock = clock;
    }

    public Optional<OwnComment> own(UUID userId) {
        return comments.own(userId);
    }

    /** Requires a confirmed email, so throwaway accounts can't fill the moderation queue. */
    public OwnComment save(UUID userId, String body, int rating) {
        User user = users.get(userId);
        if (user.emailVerifiedAt() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "email_not_verified", "Confirm your email to leave a review");
        }
        if (user.bannedAt() != null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "account_banned", "This account has been blocked");
        }
        if (!settings.load().commentsEnabled()) {
            throw ApiException.conflict("comments_disabled", "Reviews are closed at the moment");
        }
        comments.upsert(userId, body.strip(), rating);
        return comments.own(userId).orElseThrow();
    }

    public void deleteOwn(UUID userId) {
        comments.deleteOwn(userId);
    }

    public List<AdminComment> forModeration(Status status) {
        return comments.forModeration(status == null ? null : status.name());
    }

    public void moderate(UUID id, Moderation status) {
        if (comments.setStatus(id, status.name(), Instant.now(clock)) == 0) {
            throw ApiException.notFound("Comment");
        }
    }

    public void delete(UUID id) {
        if (comments.delete(id) == 0) {
            throw ApiException.notFound("Comment");
        }
    }
}
