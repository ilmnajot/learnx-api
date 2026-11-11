package uz.learnx.learnxapi.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class GameDto {
    private GameDto() {
    }

    public enum Game {
        PHRASE_MATCH,
        WORD_ORDER,
        SHADOWING
    }

    public record ResultRequest(
            @NotNull Game game,
            @NotNull @Min(0) @Max(50) Integer correct,
            @NotNull @Min(1) @Max(50) Integer total
    ) {
        @AssertTrue(message = "correct must not exceed total")
        public boolean isScoreConsistent() {
            return correct == null || total == null || correct <= total;
        }
    }

    public record Result(int points, int gamesScoredToday) {
    }
}
