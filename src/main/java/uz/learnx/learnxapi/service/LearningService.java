package uz.learnx.learnxapi.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.learnx.learnxapi.dto.LearningDto;
import uz.learnx.learnxapi.model.ApiResponse;

public interface LearningService {
    ApiResponse topics(UUID userId);

    ApiResponse lesson(UUID userId, UUID lessonId);

    ApiResponse complete(UUID userId, UUID lessonId);

    LearningDto.Lesson requireLesson(UUID lessonId);

    Optional<LearningDto.LessonSummary> nextLesson(UUID userId);

    List<String> completedCanDos(UUID userId);

    int lessonCount(UUID userId);

    /** The whole Arabic alphabet, for quizzes that need other letters as wrong answers. */
    ApiResponse letters();
}
