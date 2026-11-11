package uz.learnx.learnxapi.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.learnx.learnxapi.dto.LearningDto;
import uz.learnx.learnxapi.event.PracticeRecorded;
import uz.learnx.learnxapi.exception.ApiException;
import uz.learnx.learnxapi.mapper.LearningMapper;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.service.LearningService;
import uz.learnx.learnxapi.service.ReviewService;

@Service
@RequiredArgsConstructor
public class LearningServiceImpl implements LearningService {

    static final int LESSON_POINTS = 10;

    private final LearningMapper learningMapper;
    private final ReviewService reviewService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse topics(UUID userId) {
        Map<UUID, List<LearningDto.LessonSummary>> byTopic = this.learningMapper.lessonsWithProgress(userId).stream()
                .collect(Collectors.groupingBy(LearningDto.LessonSummary::topicId));
        List<LearningDto.TopicView> topics = this.learningMapper.topics().stream()
                .map(t -> new LearningDto.TopicView(t.id(), t.title(), byTopic.getOrDefault(t.id(), List.of())))
                .toList();
        return ApiResponse.ok(topics);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse lesson(UUID userId, UUID lessonId) {
        LearningDto.Lesson lesson = requireLesson(lessonId);
        boolean completed = this.learningMapper.lessonsWithProgress(userId).stream()
                .anyMatch(l -> l.id().equals(lessonId) && l.completedAt() != null);
        return ApiResponse.ok(new LearningDto.LessonDetail(lesson, this.learningMapper.phrases(lessonId), completed));
    }

    /** Idempotent: repeating a lesson is welcome practice, but only the first completion earns points. */
    @Override
    @Transactional
    public ApiResponse complete(UUID userId, UUID lessonId) {
        LearningDto.Lesson lesson = requireLesson(lessonId);
        boolean first = this.learningMapper.markCompleted(userId, lessonId, Instant.now(this.clock)) == 1;
        if (first) {
            this.reviewService.schedule(userId,
                    this.learningMapper.phrases(lessonId).stream().map(LearningDto.Phrase::id).toList());
        }
        this.events.publishEvent(
                new PracticeRecorded(userId, PracticeRecorded.Source.LESSON, first ? LESSON_POINTS : 0));
        return ApiResponse.ok(new LearningDto.CompletionResult(first, lesson.canDo()));
    }

    @Override
    public LearningDto.Lesson requireLesson(UUID lessonId) {
        return this.learningMapper.findLesson(lessonId).orElseThrow(() -> ApiException.notFound("Lesson"));
    }

    @Override
    public Optional<LearningDto.LessonSummary> nextLesson(UUID userId) {
        return this.learningMapper.nextLesson(userId);
    }

    @Override
    public List<String> completedCanDos(UUID userId) {
        return this.learningMapper.completedCanDos(userId);
    }

    @Override
    public int lessonCount() {
        return this.learningMapper.lessonCount();
    }
}
