package uz.learnx.learnxapi.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AdminDto {
    private AdminDto() {
    }

    public record RoleRequest(@NotNull Boolean admin) {
    }

    public record VisibilityRequest(@NotNull Boolean hidden) {
    }

    public record ModerationRequest(@NotNull CommentDto.Moderation status) {
    }

    public record UserRow(UUID id, String email, String displayName, String jobRole, String level, String role,
            Instant emailVerifiedAt, Instant bannedAt, Instant createdAt, Integer lessonsCompleted,
            LocalDate lastActive) {
    }

    public record UserPage(List<UserRow> users, int total, int page, int size) {
    }

    public record LevelCount(String level, Integer count) {
    }

    public record DayCount(LocalDate day, Integer count) {
    }

    public record Totals(Integer users, Integer verified, Integer banned, Integer newThisWeek, Integer activeToday,
            Integer activeThisWeek, Integer lessonsCompleted, Integer videosCompleted, Integer squads,
            Integer pendingComments, Integer aiRequestsToday) {
    }

    public record Stats(Totals totals, int aiGlobalDailyLimit, List<LevelCount> levels, List<DayCount> signups) {
    }

    public record VideoRow(UUID id, String title, String channel, String category, String youtubeId,
            Instant hiddenAt, Integer completions) {
    }

    public record AuditRow(UUID id, String adminEmail, String action, String target, Instant createdAt) {
    }
}
