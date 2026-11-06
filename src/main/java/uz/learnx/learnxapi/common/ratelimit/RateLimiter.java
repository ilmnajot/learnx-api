package uz.learnx.learnxapi.common.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Fixed-window rate limiting. Counters live in PostgreSQL, so limits hold across server instances. */
@Service
public class RateLimiter {

    @Mapper
    public interface RateLimitMapper {
        @Select("""
                INSERT INTO rate_limits (bucket, window_start) VALUES (#{bucket}, #{windowStart})
                ON CONFLICT (bucket, window_start) DO UPDATE SET hits = rate_limits.hits + 1
                RETURNING hits
                """)
        int hit(@Param("bucket") String bucket, @Param("windowStart") Instant windowStart);

        @Delete("DELETE FROM rate_limits WHERE window_start < #{before}")
        int deleteOlderThan(Instant before);
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {}

    private final RateLimitMapper limits;
    private final Clock clock;

    RateLimiter(RateLimitMapper limits, Clock clock) {
        this.limits = limits;
        this.clock = clock;
    }

    public Decision check(String bucket, int limit, Duration window) {
        long windowSeconds = window.toSeconds();
        long now = Instant.now(clock).getEpochSecond();
        long start = now - Math.floorMod(now, windowSeconds);
        int hits = limits.hit(bucket, Instant.ofEpochSecond(start));
        return new Decision(hits <= limit, start + windowSeconds - now);
    }

    @Scheduled(fixedDelay = 3_600_000)
    void purge() {
        limits.deleteOlderThan(Instant.now(clock).minus(Duration.ofDays(1)));
    }
}
