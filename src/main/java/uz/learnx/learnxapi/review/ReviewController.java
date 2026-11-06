package uz.learnx.learnxapi.review;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.common.security.CurrentUserId;

@RestController
@Validated
@RequestMapping("/api/v1/reviews")
class ReviewController {

    record AnswerRequest(@NotNull Boolean remembered) {}

    private final ReviewService reviews;

    ReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping("/due")
    List<ReviewMapper.DueReview> due(@CurrentUserId UUID userId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return reviews.due(userId, limit);
    }

    @PostMapping("/{phraseId}")
    ReviewService.AnswerResult answer(@CurrentUserId UUID userId, @PathVariable UUID phraseId,
            @Valid @RequestBody AnswerRequest request) {
        return reviews.answer(userId, phraseId, request.remembered());
    }
}
