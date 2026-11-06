package uz.learnx.learnxapi.video;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.common.security.CurrentUserId;

@RestController
@RequestMapping("/api/v1/videos")
class VideoController {

    private final VideoService videos;

    VideoController(VideoService videos) {
        this.videos = videos;
    }

    @GetMapping
    List<VideoMapper.VideoSummary> list(@CurrentUserId UUID userId) {
        return videos.list(userId);
    }

    @GetMapping("/{id}")
    VideoService.VideoDetail detail(@CurrentUserId UUID userId, @PathVariable UUID id) {
        return videos.detail(userId, id);
    }

    @PostMapping("/{id}/complete")
    VideoService.Completion complete(@CurrentUserId UUID userId, @PathVariable UUID id) {
        return videos.complete(userId, id);
    }
}
