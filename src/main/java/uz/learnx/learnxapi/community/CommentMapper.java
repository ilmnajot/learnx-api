package uz.learnx.learnxapi.community;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommentMapper {

    /** What the public sees. Deliberately carries no email or user id. */
    record PublicComment(UUID id, String displayName, String jobRole, String level, String body, Integer rating,
            Instant updatedAt) {}

    record OwnComment(String body, Integer rating, String status, Instant updatedAt) {}

    record AdminComment(UUID id, String email, String displayName, String body, Integer rating, String status,
            Instant updatedAt) {}

    @Insert("""
            INSERT INTO comments (user_id, body, rating) VALUES (#{userId}, #{body}, #{rating})
            ON CONFLICT (user_id) DO UPDATE SET body = EXCLUDED.body, rating = EXCLUDED.rating, status = 'PENDING',
                updated_at = now(), moderated_at = NULL
            """)
    void upsert(@Param("userId") UUID userId, @Param("body") String body, @Param("rating") int rating);

    @Select("SELECT body, rating, status, updated_at FROM comments WHERE user_id = #{userId}")
    Optional<OwnComment> own(UUID userId);

    @Delete("DELETE FROM comments WHERE user_id = #{userId}")
    int deleteOwn(UUID userId);

    @Select("""
            SELECT c.id, u.display_name, u.job_role, u.level, c.body, c.rating, c.updated_at
            FROM comments c JOIN users u ON u.id = c.user_id
            WHERE c.status = 'APPROVED' AND u.banned_at IS NULL
            ORDER BY c.updated_at DESC
            LIMIT #{limit}
            """)
    List<PublicComment> approved(int limit);

    @Select("""
            <script>
            SELECT c.id, u.email, u.display_name, c.body, c.rating, c.status, c.updated_at
            FROM comments c JOIN users u ON u.id = c.user_id
            <if test="status != null">WHERE c.status = #{status}</if>
            ORDER BY c.updated_at DESC
            LIMIT 200
            </script>
            """)
    List<AdminComment> forModeration(@Param("status") String status);

    @Update("UPDATE comments SET status = #{status}, moderated_at = #{at} WHERE id = #{id}")
    int setStatus(@Param("id") UUID id, @Param("status") String status, @Param("at") Instant at);

    @Delete("DELETE FROM comments WHERE id = #{id}")
    int delete(UUID id);
}
