package uz.learnx.learnxapi.learning;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.common.security.CurrentUserId;

@RestController
@RequestMapping("/api/v1")
class LearningController {

    private final LearningService learning;

    LearningController(LearningService learning) {
        this.learning = learning;
    }

    @GetMapping("/topics")
    List<LearningService.TopicView> topics(@CurrentUserId UUID userId) {
        return learning.catalog(userId);
    }

    @GetMapping("/lessons/{id}")
    LearningService.LessonDetail lesson(@CurrentUserId UUID userId, @PathVariable UUID id) {
        return learning.lesson(userId, id);
    }

    @PostMapping("/lessons/{id}/complete")
    LearningService.CompletionResult complete(@CurrentUserId UUID userId, @PathVariable UUID id) {
        return learning.complete(userId, id);
    }
}
