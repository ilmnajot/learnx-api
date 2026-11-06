package uz.learnx.learnxapi.writing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.ai.AiQuota;
import uz.learnx.learnxapi.ai.AiTutor;
import uz.learnx.learnxapi.common.security.CurrentUserId;
import uz.learnx.learnxapi.user.UserService;

/** "Fix my message". Text is never stored or logged, because it can come from a learner's real work. */
@RestController
@RequestMapping("/api/v1/writing")
class WritingController {

    record ImproveRequest(@NotBlank @Size(max = 2000) String text) {}

    private final AiTutor tutor;
    private final AiQuota quota;
    private final UserService users;

    WritingController(AiTutor tutor, AiQuota quota, UserService users) {
        this.tutor = tutor;
        this.quota = quota;
        this.users = users;
    }

    @PostMapping("/improve")
    AiTutor.MessageFeedback improve(@CurrentUserId UUID userId, @Valid @RequestBody ImproveRequest request) {
        quota.consume(userId);
        return tutor.improveMessage(request.text().strip(), users.get(userId).nativeLanguage());
    }
}
