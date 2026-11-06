package uz.learnx.learnxapi.review;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.learnx.learnxapi.activity.PracticeRecorded;
import uz.learnx.learnxapi.common.error.ApiException;
import uz.learnx.learnxapi.review.ReviewMapper.DueReview;
import uz.learnx.learnxapi.review.ReviewMapper.ReviewRow;

@Service
public class ReviewService {

    static final int REMEMBERED_POINTS = 1;

    public record AnswerResult(int nextReviewInDays, int remainingDue) {}

    private final ReviewMapper reviews;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ReviewService(ReviewMapper reviews, ApplicationEventPublisher events, Clock clock) {
        this.reviews = reviews;
        this.events = events;
        this.clock = clock;
    }

    /** New phrases come back tomorrow, because the lesson itself was today's practice. */
    public void schedule(UUID userId, List<UUID> phraseIds) {
        if (!phraseIds.isEmpty()) {
            reviews.schedule(userId, phraseIds, Instant.now(clock).plus(Duration.ofDays(1)));
        }
    }

    public List<DueReview> due(UUID userId, int limit) {
        return reviews.due(userId, Instant.now(clock), limit);
    }

    public int dueCount(UUID userId) {
        return reviews.dueCount(userId, Instant.now(clock));
    }

    @Transactional
    public AnswerResult answer(UUID userId, UUID phraseId, boolean remembered) {
        ReviewRow row = reviews.findForUpdate(userId, phraseId).orElseThrow(() -> ApiException.notFound("Review"));
        ReviewScheduler.State next = ReviewScheduler.next(
                new ReviewScheduler.State(row.ease(), row.intervalDays(), row.repetitions(), row.lapses()), remembered);
        Instant now = Instant.now(clock);
        reviews.update(userId, phraseId, next.ease(), next.intervalDays(), next.repetitions(), next.lapses(),
                now.plus(Duration.ofDays(next.intervalDays())), now);
        events.publishEvent(new PracticeRecorded(userId, PracticeRecorded.Source.REVIEW,
                remembered ? REMEMBERED_POINTS : 0));
        return new AnswerResult(next.intervalDays(), reviews.dueCount(userId, now));
    }

    /** A random selection for games: the learner's own phrases, topped up with starter phrases. */
    public List<DueReview> gamePool(UUID userId, int size) {
        List<DueReview> pool = new ArrayList<>(reviews.learnedSample(userId, size));
        if (pool.size() < size) {
            reviews.starterSample(size * 2).stream()
                    .filter(p -> pool.stream().noneMatch(existing -> existing.phraseId().equals(p.phraseId())))
                    .limit(size - pool.size())
                    .forEach(pool::add);
        }
        return pool;
    }
}
