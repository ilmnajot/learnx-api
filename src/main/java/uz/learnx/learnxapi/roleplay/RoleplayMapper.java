package uz.learnx.learnxapi.roleplay;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RoleplayMapper {

    record Session(UUID id, UUID userId, UUID lessonId, Integer maxTurns, Integer turnCount, Instant completedAt) {}

    record Turn(Integer turnNumber, String promptLine, String userText, String improvedText, List<String> tips,
            String replyLine, String provider) {}

    @Insert("""
            INSERT INTO roleplay_sessions (id, user_id, lesson_id, max_turns)
            VALUES (#{id}, #{userId}, #{lessonId}, #{maxTurns})
            """)
    void insertSession(@Param("id") UUID id, @Param("userId") UUID userId, @Param("lessonId") UUID lessonId,
            @Param("maxTurns") int maxTurns);

    @Select("""
            SELECT id, user_id, lesson_id, max_turns, turn_count, completed_at
            FROM roleplay_sessions WHERE id = #{id} AND user_id = #{userId}
            """)
    Optional<Session> findSession(@Param("id") UUID id, @Param("userId") UUID userId);

    @Select("""
            SELECT turn_number, prompt_line, user_text, improved_text, tips, reply_line, provider
            FROM roleplay_turns WHERE session_id = #{sessionId} ORDER BY turn_number
            """)
    List<Turn> turns(UUID sessionId);

    @Insert("""
            INSERT INTO roleplay_turns
                (session_id, turn_number, prompt_line, user_text, improved_text, tips, reply_line, provider)
            VALUES (#{sessionId}, #{turn.turnNumber}, #{turn.promptLine}, #{turn.userText}, #{turn.improvedText},
                #{turn.tips,typeHandler=uz.learnx.learnxapi.common.mybatis.StringListTypeHandler},
                #{turn.replyLine}, #{turn.provider})
            """)
    void insertTurn(@Param("sessionId") UUID sessionId, @Param("turn") Turn turn);

    @Update("""
            UPDATE roleplay_sessions
            SET turn_count = #{turnNumber},
                completed_at = CASE WHEN #{turnNumber} >= max_turns THEN #{now} ELSE completed_at END
            WHERE id = #{sessionId}
            """)
    void advance(@Param("sessionId") UUID sessionId, @Param("turnNumber") int turnNumber, @Param("now") Instant now);
}
