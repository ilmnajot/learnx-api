package uz.learnx.learnxapi.game;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.common.security.CurrentUserId;
import uz.learnx.learnxapi.review.ReviewMapper.DueReview;

@RestController
@Validated
@RequestMapping("/api/v1/games")
class GameController {

    record ResultRequest(
            @NotNull GameService.Game game,
            @NotNull @Min(0) @Max(50) Integer correct,
            @NotNull @Min(1) @Max(50) Integer total) {

        @AssertTrue(message = "correct must not exceed total")
        boolean isScoreConsistent() {
            return correct == null || total == null || correct <= total;
        }
    }

    private final GameService games;

    GameController(GameService games) {
        this.games = games;
    }

    @GetMapping("/pool")
    List<DueReview> pool(@CurrentUserId UUID userId, @RequestParam(defaultValue = "8") @Min(2) @Max(20) int size) {
        return games.pool(userId, size);
    }

    @PostMapping("/results")
    GameService.Result result(@CurrentUserId UUID userId, @Valid @RequestBody ResultRequest request) {
        return games.record(userId, request.game(), request.correct(), request.total());
    }
}
