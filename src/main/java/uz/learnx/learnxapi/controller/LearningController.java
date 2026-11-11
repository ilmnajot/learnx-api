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
import uz.learnx.learnxapi.service.LearningService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LearningController {

    private final LearningService learningService;

    @GetMapping("/topics")
    public ApiResponse topics(@CurrentUserId UUID userId) {
        return this.learningService.topics(userId);
    }

    @GetMapping("/lessons/{id}")
    public ApiResponse lesson(@CurrentUserId UUID userId, @PathVariable UUID id) {
        return this.learningService.lesson(userId, id);
    }

    // the Arabic alphabet, in teaching order
    @GetMapping("/letters")
    public ApiResponse letters() {
        return this.learningService.letters();
    }

    @PostMapping("/lessons/{id}/complete")
    public ApiResponse complete(@CurrentUserId UUID userId, @PathVariable UUID id) {
        return this.learningService.complete(userId, id);
    }
}
