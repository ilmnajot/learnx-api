package uz.learnx.learnxapi.review;

import java.math.BigDecimal;
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
public interface ReviewMapper {

    record ReviewRow(BigDecimal ease, Integer intervalDays, Integer repetitions, Integer lapses) {}

    record DueReview(UUID phraseId, String english, String translation, String example) {}

    @Insert("""
            <script>
            INSERT INTO phrase_reviews (user_id, phrase_id, due_at) VALUES
            <foreach collection="phraseIds" item="phraseId" separator=",">
                (#{userId}, #{phraseId}, #{dueAt})
            </foreach>
            ON CONFLICT DO NOTHING
            </script>
            """)
    void schedule(@Param("userId") UUID userId, @Param("phraseIds") List<UUID> phraseIds, @Param("dueAt") Instant dueAt);

    @Select("""
            SELECT p.id AS phrase_id, p.english, p.translation, p.example
            FROM phrase_reviews r JOIN phrases p ON p.id = r.phrase_id
            WHERE r.user_id = #{userId} AND r.due_at <= #{now}
            ORDER BY r.due_at
            LIMIT #{limit}
            """)
    List<DueReview> due(@Param("userId") UUID userId, @Param("now") Instant now, @Param("limit") int limit);

    @Select("SELECT count(*) FROM phrase_reviews WHERE user_id = #{userId} AND due_at <= #{now}")
    int dueCount(@Param("userId") UUID userId, @Param("now") Instant now);

    @Select("""
            SELECT ease, interval_days, repetitions, lapses FROM phrase_reviews
            WHERE user_id = #{userId} AND phrase_id = #{phraseId}
            FOR UPDATE
            """)
    Optional<ReviewRow> findForUpdate(@Param("userId") UUID userId, @Param("phraseId") UUID phraseId);

    @Update("""
            UPDATE phrase_reviews
            SET ease = #{ease}, interval_days = #{intervalDays}, repetitions = #{repetitions}, lapses = #{lapses},
                due_at = #{dueAt}, reviewed_at = #{now}
            WHERE user_id = #{userId} AND phrase_id = #{phraseId}
            """)
    void update(@Param("userId") UUID userId, @Param("phraseId") UUID phraseId, @Param("ease") BigDecimal ease,
            @Param("intervalDays") int intervalDays, @Param("repetitions") int repetitions,
            @Param("lapses") int lapses, @Param("dueAt") Instant dueAt, @Param("now") Instant now);

    /** Phrases the learner has met, in random order, for games. */
    @Select("""
            SELECT p.id AS phrase_id, p.english, p.translation, p.example
            FROM phrase_reviews r JOIN phrases p ON p.id = r.phrase_id
            WHERE r.user_id = #{userId}
            ORDER BY random()
            LIMIT #{limit}
            """)
    List<DueReview> learnedSample(@Param("userId") UUID userId, @Param("limit") int limit);

    /** Starter phrases for learners with too few of their own. */
    @Select("""
            SELECT p.id AS phrase_id, p.english, p.translation, p.example
            FROM phrases p JOIN lessons l ON l.id = p.lesson_id
            ORDER BY l.position, p.position
            LIMIT #{limit}
            """)
    List<DueReview> starterSample(int limit);
}
