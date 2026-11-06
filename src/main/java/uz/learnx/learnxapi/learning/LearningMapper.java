package uz.learnx.learnxapi.learning;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LearningMapper {

    record Topic(UUID id, String title) {}

    record LessonSummary(UUID id, UUID topicId, String title, String canDo, Integer estMinutes, Instant completedAt) {}

    record Lesson(UUID id, String title, String canDo, String scenario, String aiPersona, String openingLine,
            Integer estMinutes) {}

    record Phrase(UUID id, String english, String translation, String example) {}

    @Select("SELECT id, title FROM topics ORDER BY position")
    List<Topic> topics();

    @Select("""
            SELECT l.id, l.topic_id, l.title, l.can_do, l.est_minutes, p.completed_at
            FROM lessons l
            LEFT JOIN lesson_progress p ON p.lesson_id = l.id AND p.user_id = #{userId}
            ORDER BY l.position
            """)
    List<LessonSummary> lessonsWithProgress(UUID userId);

    @Select("""
            SELECT l.id, l.topic_id, l.title, l.can_do, l.est_minutes, NULL::timestamptz AS completed_at
            FROM lessons l
            WHERE NOT EXISTS (SELECT 1 FROM lesson_progress p WHERE p.lesson_id = l.id AND p.user_id = #{userId})
            ORDER BY l.position
            LIMIT 1
            """)
    Optional<LessonSummary> nextLesson(UUID userId);

    @Select("""
            SELECT id, title, can_do, scenario, ai_persona, opening_line, est_minutes
            FROM lessons WHERE id = #{id}
            """)
    Optional<Lesson> findLesson(UUID id);

    @Select("""
            SELECT id, english, translation, example FROM phrases
            WHERE lesson_id = #{lessonId} ORDER BY position
            """)
    List<Phrase> phrases(UUID lessonId);

    /** @return 1 the first time a lesson is completed, otherwise 0 */
    @Insert("""
            INSERT INTO lesson_progress (user_id, lesson_id, completed_at) VALUES (#{userId}, #{lessonId}, #{at})
            ON CONFLICT DO NOTHING
            """)
    int markCompleted(@Param("userId") UUID userId, @Param("lessonId") UUID lessonId, @Param("at") Instant at);

    @Select("""
            SELECT l.can_do FROM lesson_progress p JOIN lessons l ON l.id = p.lesson_id
            WHERE p.user_id = #{userId} ORDER BY p.completed_at DESC
            """)
    List<String> completedCanDos(UUID userId);

    @Select("SELECT count(*) FROM lessons")
    int lessonCount();
}
