package uz.learnx.learnxapi.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class LearningDto {
    private LearningDto() {
    }

    public record Topic(UUID id, String title) {
    }

    public record LessonSummary(UUID id, UUID topicId, String title, String canDo, Integer estMinutes,
            Instant completedAt) {
    }

    public record Lesson(UUID id, String title, String canDo, String scenario, String aiPersona, String openingLine,
            Integer estMinutes) {
    }

    public record Phrase(UUID id, String english, String translation, String example) {
    }

    public record TopicView(UUID id, String title, List<LessonSummary> lessons) {
    }

    public record LessonDetail(Lesson lesson, List<Phrase> phrases, boolean completed) {
    }

    public record CompletionResult(boolean firstCompletion, String canDo) {
    }
}
