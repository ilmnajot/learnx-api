package uz.learnx.learnxapi.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.learnx.learnxapi.dto.ReviewDto;
import uz.learnx.learnxapi.event.PracticeRecorded;
import uz.learnx.learnxapi.exception.ApiException;
import uz.learnx.learnxapi.mapper.ReviewMapper;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.service.ReviewService;
import uz.learnx.learnxapi.utils.ReviewScheduler;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    static final int REMEMBERED_POINTS = 1;

    private final ReviewMapper reviewMapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    public ApiResponse due(UUID userId, int limit) {
        return ApiResponse.ok(this.reviewMapper.due(userId, Instant.now(this.clock), limit));
    }

    @Override
    @Transactional
    public ApiResponse answer(UUID userId, UUID phraseId, ReviewDto.AnswerRequest request) {
        boolean remembered = request.remembered();
        ReviewDto.ReviewRow row = this.reviewMapper.findForUpdate(userId, phraseId)
                .orElseThrow(() -> ApiException.notFound("Review"));
        ReviewScheduler.State next = ReviewScheduler.next(
                new ReviewScheduler.State(row.ease(), row.intervalDays(), row.repetitions(), row.lapses()), remembered);
        Instant now = Instant.now(this.clock);
        this.reviewMapper.update(userId, phraseId, next.ease(), next.intervalDays(), next.repetitions(), next.lapses(),
                now.plus(Duration.ofDays(next.intervalDays())), now);
        this.events.publishEvent(new PracticeRecorded(userId, PracticeRecorded.Source.REVIEW,
                remembered ? REMEMBERED_POINTS : 0));
        return ApiResponse.ok(new ReviewDto.AnswerResult(next.intervalDays(), this.reviewMapper.dueCount(userId, now)));
    }

    @Override
    public void schedule(UUID userId, List<UUID> phraseIds) {
        if (!phraseIds.isEmpty()) {
            this.reviewMapper.schedule(userId, phraseIds, Instant.now(this.clock).plus(Duration.ofDays(1)));
        }
    }

    @Override
    public int dueCount(UUID userId) {
        return this.reviewMapper.dueCount(userId, Instant.now(this.clock));
    }

    @Override
    public List<ReviewDto.DueReview> gamePool(UUID userId, int size) {
        List<ReviewDto.DueReview> pool = new ArrayList<>(this.reviewMapper.learnedSample(userId, size));
        if (pool.size() < size) {
            this.reviewMapper.starterSample(size * 2).stream()
                    .filter(p -> pool.stream().noneMatch(existing -> existing.phraseId().equals(p.phraseId())))
                    .limit(size - pool.size())
                    .forEach(pool::add);
        }
        return pool;
    }
}
