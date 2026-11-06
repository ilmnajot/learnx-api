package uz.learnx.learnxapi.community;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.common.security.CurrentUserId;

@RestController
@RequestMapping("/api/v1")
class CommunityController {

    record CommentRequest(
            @NotBlank @Size(min = 10, max = 500) String body,
            @NotNull @Min(1) @Max(5) Integer rating) {}

    private final PublicSite site;
    private final CommentService comments;

    CommunityController(PublicSite site, CommentService comments) {
        this.site = site;
        this.comments = comments;
    }

    @GetMapping("/public/site")
    PublicSite.View site() {
        return site.view();
    }

    /** 204 means the learner hasn't written a review yet. */
    @GetMapping("/comments/me")
    ResponseEntity<CommentMapper.OwnComment> mine(@CurrentUserId UUID userId) {
        return comments.own(userId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/comments/me")
    CommentMapper.OwnComment save(@CurrentUserId UUID userId, @Valid @RequestBody CommentRequest request) {
        return comments.save(userId, request.body(), request.rating());
    }

    @DeleteMapping("/comments/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@CurrentUserId UUID userId) {
        comments.deleteOwn(userId);
    }
}
