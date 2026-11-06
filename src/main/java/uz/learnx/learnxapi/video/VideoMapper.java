package uz.learnx.learnxapi.video;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import uz.learnx.learnxapi.learning.LearningMapper.Phrase;

@Mapper
public interface VideoMapper {

    record VideoSummary(UUID id, String youtubeId, String title, String channel, String category,
            Integer durationSeconds, String whyWatch, String speakPrompt, Instant completedAt) {}

    @Select("""
            SELECT v.id, v.youtube_id, v.title, v.channel, v.category, v.duration_seconds, v.why_watch,
                   v.speak_prompt, p.completed_at
            FROM videos v
            LEFT JOIN video_progress p ON p.video_id = v.id AND p.user_id = #{userId}
            ORDER BY v.position
            """)
    List<VideoSummary> list(UUID userId);

    @Select("""
            SELECT v.id, v.youtube_id, v.title, v.channel, v.category, v.duration_seconds, v.why_watch,
                   v.speak_prompt, p.completed_at
            FROM videos v
            LEFT JOIN video_progress p ON p.video_id = v.id AND p.user_id = #{userId}
            WHERE v.id = #{videoId}
            """)
    Optional<VideoSummary> find(@Param("userId") UUID userId, @Param("videoId") UUID videoId);

    @Select("SELECT id, english, translation, example FROM phrases WHERE video_id = #{videoId} ORDER BY position")
    List<Phrase> phrases(UUID videoId);

    /** @return 1 the first time a video is completed, otherwise 0 */
    @Insert("""
            INSERT INTO video_progress (user_id, video_id, completed_at) VALUES (#{userId}, #{videoId}, #{at})
            ON CONFLICT DO NOTHING
            """)
    int markCompleted(@Param("userId") UUID userId, @Param("videoId") UUID videoId, @Param("at") Instant at);
}
