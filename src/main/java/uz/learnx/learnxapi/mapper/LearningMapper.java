package uz.learnx.learnxapi.mapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import uz.learnx.learnxapi.dto.LearningDto;

@Mapper
public interface LearningMapper {

    List<LearningDto.Topic> topics();

    List<LearningDto.LessonSummary> lessonsWithProgress(@Param("userId") UUID userId);

    Optional<LearningDto.LessonSummary> nextLesson(@Param("userId") UUID userId);

    Optional<LearningDto.Lesson> findLesson(@Param("id") UUID id);

    List<LearningDto.Phrase> phrases(@Param("lessonId") UUID lessonId);

    /** @return 1 the first time a lesson is completed, otherwise 0 */
    int markCompleted(@Param("userId") UUID userId, @Param("lessonId") UUID lessonId, @Param("at") Instant at);

    List<String> completedCanDos(@Param("userId") UUID userId);

    int lessonCount();
}
