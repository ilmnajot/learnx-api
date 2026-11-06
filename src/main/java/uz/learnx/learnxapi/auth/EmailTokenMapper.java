package uz.learnx.learnxapi.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EmailTokenMapper {

    @Insert("""
            INSERT INTO email_tokens (user_id, purpose, token_hash, expires_at, created_at)
            VALUES (#{userId}, #{purpose}, #{tokenHash}, #{expiresAt}, #{now})
            """)
    void insert(@Param("userId") UUID userId, @Param("purpose") String purpose, @Param("tokenHash") String tokenHash,
            @Param("expiresAt") Instant expiresAt, @Param("now") Instant now);

    /** Only the newest link should work, so older unused ones are retired when a new one is issued. */
    @Update("""
            UPDATE email_tokens SET used_at = #{now}
            WHERE user_id = #{userId} AND purpose = #{purpose} AND used_at IS NULL
            """)
    void retireOpen(@Param("userId") UUID userId, @Param("purpose") String purpose, @Param("now") Instant now);

    @Select("""
            SELECT count(*) FROM email_tokens
            WHERE user_id = #{userId} AND purpose = #{purpose} AND created_at > #{since}
            """)
    int countSince(@Param("userId") UUID userId, @Param("purpose") String purpose, @Param("since") Instant since);

    /** Atomic, so a link can't be used twice even by simultaneous requests. */
    @Select("""
            UPDATE email_tokens SET used_at = #{now}
            WHERE token_hash = #{tokenHash} AND purpose = #{purpose} AND used_at IS NULL AND expires_at > #{now}
            RETURNING user_id
            """)
    Optional<UUID> consume(@Param("tokenHash") String tokenHash, @Param("purpose") String purpose,
            @Param("now") Instant now);

    @Delete("DELETE FROM email_tokens WHERE expires_at < #{before}")
    int deleteExpired(Instant before);
}
