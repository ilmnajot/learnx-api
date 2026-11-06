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
public interface RefreshTokenMapper {

    record RefreshToken(UUID id, UUID userId, Instant expiresAt, Instant revokedAt, Instant rotatedAt) {}

    @Insert("INSERT INTO refresh_tokens (user_id, token_hash, expires_at) VALUES (#{userId}, #{tokenHash}, #{expiresAt})")
    void insert(@Param("userId") UUID userId, @Param("tokenHash") String tokenHash,
            @Param("expiresAt") Instant expiresAt);

    @Select("""
            SELECT id, user_id, expires_at, revoked_at, rotated_at
            FROM refresh_tokens WHERE token_hash = #{tokenHash} FOR UPDATE
            """)
    Optional<RefreshToken> findForUpdate(String tokenHash);

    @Update("UPDATE refresh_tokens SET revoked_at = #{at}, rotated_at = #{at} WHERE id = #{id} AND revoked_at IS NULL")
    void rotate(@Param("id") UUID id, @Param("at") Instant at);

    @Update("UPDATE refresh_tokens SET revoked_at = #{at} WHERE id = #{id} AND revoked_at IS NULL")
    void revoke(@Param("id") UUID id, @Param("at") Instant at);

    @Update("UPDATE refresh_tokens SET revoked_at = #{at} WHERE user_id = #{userId} AND revoked_at IS NULL")
    void revokeAllForUser(@Param("userId") UUID userId, @Param("at") Instant at);

    @Delete("DELETE FROM refresh_tokens WHERE expires_at < #{before}")
    int deleteExpired(Instant before);
}
