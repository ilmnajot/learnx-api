package uz.learnx.learnxapi.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public final class ReviewDto {
    private ReviewDto() {
    }

    public record ReviewRow(BigDecimal ease, Integer intervalDays, Integer repetitions, Integer lapses) {
    }

    public record DueReview(UUID phraseId, String english, String translation, String example) {
    }

    public record AnswerRequest(@NotNull Boolean remembered) {
    }

    public record AnswerResult(int nextReviewInDays, int remainingDue) {
    }
}
