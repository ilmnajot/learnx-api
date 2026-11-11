package uz.learnx.learnxapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** The public homepage and the settings the admin controls at runtime. */
public final class SiteDto {
    private SiteDto() {
    }

    /**
     * @param statsMinLearners public statistics stay hidden until this many learners exist, because a homepage saying
     *     "3 learners" works against you. Set it to 0 to always show them.
     */
    public record Settings(
            @NotNull Boolean donationEnabled,
            @Pattern(regexp = "^$|^[0-9]{16}$", message = "Card number must be 16 digits") String donationCard,
            @Size(max = 60) String donationHolder,
            @Size(max = 300) String donationMessage,
            @NotNull Boolean commentsEnabled,
            @NotNull @Min(0) @Max(1_000_000) Integer statsMinLearners
    ) {
    }

    public record SettingRow(String key, String value) {
    }

    public record LevelCount(String level, Integer learners) {
    }

    public record Totals(Integer learners, Integer activeThisWeek, Integer lessonsCompleted, Integer practiceDays,
            Integer squads) {
    }

    public record Stats(Totals totals, List<LevelCount> levels) {
    }

    public record Donation(String card, String holder, String message) {
    }

    public record View(Stats stats, List<CommentDto.PublicComment> comments, Boolean commentsEnabled,
            Donation donation) {
    }
}
