package uz.learnx.learnxapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RoleplayDto {
    private RoleplayDto() {
    }

    public record Session(UUID id, UUID userId, UUID lessonId, Integer maxTurns, Integer turnCount,
            Instant completedAt) {
    }

    public record Turn(Integer turnNumber, String promptLine, String userText, String improvedText, List<String> tips,
            String replyLine, String provider) {
    }

    public record TurnRequest(@NotBlank @Size(max = 500) String text) {
    }

    public record Started(UUID sessionId, String openingLine, String scenario, int maxTurns) {
    }

    public record TurnResult(int turnNumber, String improved, List<String> tips, String reply, boolean finished,
            String provider) {
    }
}
