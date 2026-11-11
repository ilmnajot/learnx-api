package uz.learnx.learnxapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class CommentDto {
    private CommentDto() {
    }

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }

    public enum Moderation {
        APPROVED,
        REJECTED
    }

    public record CommentRequest(
            @NotBlank @Size(min = 10, max = 500) String body,
            @NotNull @Min(1) @Max(5) Integer rating
    ) {
    }

    /** What the public sees. Deliberately carries no email or user id. */
    public record PublicComment(UUID id, String displayName, String jobRole, String level, String body,
            Integer rating, Instant updatedAt) {
    }

    public record OwnComment(String body, Integer rating, String status, Instant updatedAt) {
    }

    public record AdminComment(UUID id, String email, String displayName, String body, Integer rating, String status,
            Instant updatedAt) {
    }
}
