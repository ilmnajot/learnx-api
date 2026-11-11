package uz.learnx.learnxapi.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.dto.CommentDto;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.security.CurrentUserId;
import uz.learnx.learnxapi.service.CommentService;
import uz.learnx.learnxapi.service.PublicSiteService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommunityController {

    private final PublicSiteService publicSiteService;
    private final CommentService commentService;

    // public — statistics, approved reviews and donation details for the homepage
    @GetMapping("/public/site")
    public ApiResponse site() {
        return this.publicSiteService.site();
    }

    // authenticated — no data means the learner hasn't written a review yet
    @GetMapping("/comments/me")
    public ApiResponse mine(@CurrentUserId UUID userId) {
        return this.commentService.mine(userId);
    }

    // authenticated — saving again sends the review back to moderation
    @PutMapping("/comments/me")
    public ApiResponse save(@CurrentUserId UUID userId, @Valid @RequestBody CommentDto.CommentRequest request) {
        return this.commentService.save(userId, request);
    }

    // authenticated
    @DeleteMapping("/comments/me")
    public ApiResponse delete(@CurrentUserId UUID userId) {
        return this.commentService.deleteMine(userId);
    }
}
