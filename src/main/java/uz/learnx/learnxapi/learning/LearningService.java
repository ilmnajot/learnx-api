package uz.learnx.learnxapi.learning;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.learnx.learnxapi.activity.PracticeRecorded;
import uz.learnx.learnxapi.common.error.ApiException;
import uz.learnx.learnxapi.learning.LearningMapper.Lesson;
import uz.learnx.learnxapi.learning.LearningMapper.LessonSummary;
import uz.learnx.learnxapi.learning.LearningMapper.Phrase;
import uz.learnx.learnxapi.review.ReviewService;

@Service
public class LearningService {

    static final int LESSON_POINTS = 10;

    public record TopicView(UUID id, String title, List<LessonSummary> lessons) {}

    public record LessonDetail(Lesson lesson, List<Phrase> phrases, boolean completed) {}

    public record CompletionResult(boolean firstCompletion, String canDo) {}

    private final LearningMapper learning;
    private final ReviewService reviews;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    LearningService(LearningMapper learning, ReviewService reviews, ApplicationEventPublisher events, Clock clock) {
        this.learning = learning;
        this.reviews = reviews;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<TopicView> catalog(UUID userId) {
        Map<UUID, List<LessonSummary>> byTopic = learning.lessonsWithProgress(userId).stream()
                .collect(Collectors.groupingBy(LessonSummary::topicId));
        return learning.topics().stream()
                .map(t -> new TopicView(t.id(), t.title(), byTopic.getOrDefault(t.id(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public LessonDetail lesson(UUID userId, UUID lessonId) {
        Lesson lesson = requireLesson(lessonId);
        boolean completed = learning.lessonsWithProgress(userId).stream()
                .anyMatch(l -> l.id().equals(lessonId) && l.completedAt() != null);
        return new LessonDetail(lesson, learning.phrases(lessonId), completed);
    }

    public Lesson requireLesson(UUID lessonId) {
        return learning.findLesson(lessonId).orElseThrow(() -> ApiException.notFound("Lesson"));
    }

    public Optional<LessonSummary> nextLesson(UUID userId) {
        return learning.nextLesson(userId);
    }

    /** Idempotent: repeating a lesson is welcome practice, but only the first completion earns points. */
    @Transactional
    public CompletionResult complete(UUID userId, UUID lessonId) {
        Lesson lesson = requireLesson(lessonId);
        boolean first = learning.markCompleted(userId, lessonId, Instant.now(clock)) == 1;
        if (first) {
            reviews.schedule(userId, learning.phrases(lessonId).stream().map(Phrase::id).toList());
        }
        events.publishEvent(new PracticeRecorded(userId, PracticeRecorded.Source.LESSON, first ? LESSON_POINTS : 0));
        return new CompletionResult(first, lesson.canDo());
    }

    public List<String> completedCanDos(UUID userId) {
        return learning.completedCanDos(userId);
    }

    public int lessonCount() {
        return learning.lessonCount();
    }
}
