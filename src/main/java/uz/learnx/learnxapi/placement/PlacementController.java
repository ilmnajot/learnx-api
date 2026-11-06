package uz.learnx.learnxapi.placement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.common.security.CurrentUserId;

@RestController
@RequestMapping("/api/v1/placement")
class PlacementController {

    /** Deliberately omits the correct answer. */
    record QuestionView(UUID id, String prompt, List<String> options) {}

    record Answer(@NotNull UUID questionId, @NotNull Integer selectedIndex) {}

    record SubmitRequest(@NotEmpty @Size(max = 50) List<@Valid @NotNull Answer> answers) {}

    private final PlacementService placement;

    PlacementController(PlacementService placement) {
        this.placement = placement;
    }

    @GetMapping("/questions")
    List<QuestionView> questions() {
        return placement.questions().stream().map(q -> new QuestionView(q.id(), q.prompt(), q.options())).toList();
    }

    @PostMapping("/submit")
    PlacementService.Result submit(@CurrentUserId UUID userId, @Valid @RequestBody SubmitRequest request) {
        Map<UUID, Integer> answers = request.answers().stream()
                .collect(Collectors.toMap(Answer::questionId, Answer::selectedIndex, (first, second) -> first));
        return placement.submit(userId, answers);
    }
}
