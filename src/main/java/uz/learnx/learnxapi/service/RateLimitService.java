package uz.learnx.learnxapi.service;

import java.time.Duration;
import uz.learnx.learnxapi.dto.RateLimitDto;

/** Fixed-window rate limiting. Counters live in PostgreSQL, so limits hold across server instances. */
public interface RateLimitService {
    RateLimitDto.Decision check(String bucket, int limit, Duration window);
}
