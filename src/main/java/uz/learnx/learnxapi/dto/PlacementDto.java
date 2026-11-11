package uz.learnx.learnxapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class PlacementDto {
    private PlacementDto() {
    }

    public record Question(UUID id, String prompt, List<String> options, Integer correctIndex, Integer weight) {
    }

    /** Deliberately omits the correct answer. */
    public record QuestionView(UUID id, String prompt, List<String> options) {
    }

    public record Answer(@NotNull UUID questionId, @NotNull Integer selectedIndex) {
    }

    public record SubmitRequest(@NotEmpty @Size(max = 50) List<@Valid @NotNull Answer> answers) {
    }

    public record Result(String level, int score, int maxScore) {
    }
}
