package uz.learnx.learnxapi.video;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.learnx.learnxapi.activity.PracticeRecorded;
import uz.learnx.learnxapi.common.error.ApiException;
import uz.learnx.learnxapi.learning.LearningMapper.Phrase;
import uz.learnx.learnxapi.review.ReviewService;
import uz.learnx.learnxapi.video.VideoMapper.VideoSummary;

@Service
public class VideoService {

    /** Worth less than a lesson: the server can't confirm a video was watched, and watching is the easy part. */
    static final int VIDEO_POINTS = 3;

    public record VideoDetail(VideoSummary video, List<Phrase> phrases) {}

    public record Completion(boolean firstCompletion, int phrasesScheduled) {}

    private final VideoMapper videos;
    private final ReviewService reviews;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    VideoService(VideoMapper videos, ReviewService reviews, ApplicationEventPublisher events, Clock clock) {
        this.videos = videos;
        this.reviews = reviews;
        this.events = events;
        this.clock = clock;
    }

    public List<VideoSummary> list(UUID userId) {
        return videos.list(userId);
    }

    @Transactional(readOnly = true)
    public VideoDetail detail(UUID userId, UUID videoId) {
        return new VideoDetail(require(userId, videoId), videos.phrases(videoId));
    }

    /** Idempotent: rewatching is welcome, but only the first completion earns points and schedules phrases. */
    @Transactional
    public Completion complete(UUID userId, UUID videoId) {
        require(userId, videoId);
        boolean first = videos.markCompleted(userId, videoId, Instant.now(clock)) == 1;
        int scheduled = 0;
        if (first) {
            List<UUID> phraseIds = videos.phrases(videoId).stream().map(Phrase::id).toList();
            reviews.schedule(userId, phraseIds);
            scheduled = phraseIds.size();
        }
        events.publishEvent(new PracticeRecorded(userId, PracticeRecorded.Source.VIDEO, first ? VIDEO_POINTS : 0));
        return new Completion(first, scheduled);
    }

    private VideoSummary require(UUID userId, UUID videoId) {
        return videos.find(userId, videoId).orElseThrow(() -> ApiException.notFound("Video"));
    }
}
