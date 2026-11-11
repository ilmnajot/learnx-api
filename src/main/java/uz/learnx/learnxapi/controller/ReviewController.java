package uz.learnx.learnxapi.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.dto.ReviewDto;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.security.CurrentUserId;
import uz.learnx.learnxapi.service.ReviewService;

@RestController
@Validated
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/due")
    public ApiResponse due(@CurrentUserId UUID userId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return this.reviewService.due(userId, limit);
    }

    // body: {"remembered": true}
    @PostMapping("/{phraseId}")
    public ApiResponse answer(@CurrentUserId UUID userId, @PathVariable UUID phraseId,
            @Valid @RequestBody ReviewDto.AnswerRequest request) {
        return this.reviewService.answer(userId, phraseId, request);
    }
}
