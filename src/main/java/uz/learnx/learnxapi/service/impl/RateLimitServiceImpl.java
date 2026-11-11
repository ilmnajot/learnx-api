package uz.learnx.learnxapi.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uz.learnx.learnxapi.dto.RateLimitDto;
import uz.learnx.learnxapi.mapper.RateLimitMapper;
import uz.learnx.learnxapi.service.RateLimitService;

@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private final RateLimitMapper rateLimitMapper;
    private final Clock clock;

    @Override
    public RateLimitDto.Decision check(String bucket, int limit, Duration window) {
        long windowSeconds = window.toSeconds();
        long now = Instant.now(this.clock).getEpochSecond();
        long start = now - Math.floorMod(now, windowSeconds);
        int hits = this.rateLimitMapper.hit(bucket, Instant.ofEpochSecond(start));
        return new RateLimitDto.Decision(hits <= limit, start + windowSeconds - now);
    }

    @Scheduled(fixedDelay = 3_600_000)
    public void purge() {
        this.rateLimitMapper.deleteOlderThan(Instant.now(this.clock).minus(Duration.ofDays(1)));
    }
}
