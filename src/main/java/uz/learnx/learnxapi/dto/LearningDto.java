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

    /** {@code letter} is only present for lessons of the Arabic alphabet course. */
    public record LessonDetail(Lesson lesson, List<Phrase> phrases, boolean completed, Letter letter) {
    }

    /** One letter of the Arabic alphabet, with its four shapes. */
    public record Letter(UUID lessonId, String glyph, String name, String latin, String hint, String isolatedForm,
            String initialForm, String medialForm, String finalForm, String say, String word, String wordLatin,
            String wordMeaning, List<String> similar, String audioUrl) {
    }

    public record CompletionResult(boolean firstCompletion, String canDo) {
    }
}
