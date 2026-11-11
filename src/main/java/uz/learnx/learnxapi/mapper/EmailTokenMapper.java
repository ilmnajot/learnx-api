package uz.learnx.learnxapi.mapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmailTokenMapper {

    void insert(@Param("userId") UUID userId, @Param("purpose") String purpose, @Param("tokenHash") String tokenHash,
            @Param("expiresAt") Instant expiresAt, @Param("now") Instant now);

    /** Only the newest link should work, so older unused ones are retired when a new one is issued. */
    void retireOpen(@Param("userId") UUID userId, @Param("purpose") String purpose, @Param("now") Instant now);

    int countSince(@Param("userId") UUID userId, @Param("purpose") String purpose, @Param("since") Instant since);

    /** Atomic, so a link can't be used twice even by simultaneous requests. */
    Optional<UUID> consume(@Param("tokenHash") String tokenHash, @Param("purpose") String purpose,
            @Param("now") Instant now);

    int deleteExpired(@Param("before") Instant before);
}
