package uz.learnx.learnxapi.mapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import uz.learnx.learnxapi.dto.AuthDto;

@Mapper
public interface RefreshTokenMapper {

    void insert(@Param("userId") UUID userId, @Param("tokenHash") String tokenHash,
            @Param("expiresAt") Instant expiresAt);

    Optional<AuthDto.RefreshToken> findForUpdate(@Param("tokenHash") String tokenHash);

    void rotate(@Param("id") UUID id, @Param("at") Instant at);

    void revoke(@Param("id") UUID id, @Param("at") Instant at);

    void revokeAllForUser(@Param("userId") UUID userId, @Param("at") Instant at);

    int deleteExpired(@Param("before") Instant before);
}
