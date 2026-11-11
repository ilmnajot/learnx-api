package uz.learnx.learnxapi.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.security.CurrentUserId;
import uz.learnx.learnxapi.service.VideoService;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @GetMapping
    public ApiResponse list(@CurrentUserId UUID userId) {
        return this.videoService.list(userId);
    }

    @GetMapping("/{id}")
    public ApiResponse detail(@CurrentUserId UUID userId, @PathVariable UUID id) {
        return this.videoService.detail(userId, id);
    }

    @PostMapping("/{id}/complete")
    public ApiResponse complete(@CurrentUserId UUID userId, @PathVariable UUID id) {
        return this.videoService.complete(userId, id);
    }
}
