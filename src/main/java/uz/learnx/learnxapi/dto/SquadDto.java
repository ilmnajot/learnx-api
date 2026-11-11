package uz.learnx.learnxapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class SquadDto {
    private SquadDto() {
    }

    public record Squad(UUID id, String name, String inviteCode, UUID ownerId) {
    }

    public record Incident(UUID id, String title, Integer points) {
    }

    public record MemberRow(UUID userId, String displayName, Integer weekPoints, Boolean practisedToday) {
    }

    public record CreateRequest(@NotBlank @Size(max = 40) String name) {
    }

    public record JoinRequest(@NotBlank @Size(max = 16) String inviteCode) {
    }

    public record IncidentView(String title, int points, int target, int progressPercent, boolean resolved) {
    }

    public record SquadView(UUID id, String name, String inviteCode, boolean owner, int streak, int practisedToday,
            int requiredToday, boolean streakSafeToday, IncidentView incident, List<MemberRow> members) {
    }
}
