package uz.learnx.learnxapi.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.learnx.learnxapi.dto.LearningDto;
import uz.learnx.learnxapi.dto.VideoDto;
import uz.learnx.learnxapi.event.PracticeRecorded;
import uz.learnx.learnxapi.exception.ApiException;
import uz.learnx.learnxapi.mapper.VideoMapper;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.service.ReviewService;
import uz.learnx.learnxapi.service.VideoService;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    /** Worth less than a lesson: the server can't confirm a video was watched, and watching is the easy part. */
    static final int VIDEO_POINTS = 3;

    private final VideoMapper videoMapper;
    private final ReviewService reviewService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    public ApiResponse list(UUID userId) {
        return ApiResponse.ok(this.videoMapper.list(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse detail(UUID userId, UUID videoId) {
        return ApiResponse.ok(new VideoDto.VideoDetail(require(userId, videoId), this.videoMapper.phrases(videoId)));
    }

    /** Idempotent: rewatching is welcome, but only the first completion earns points and schedules phrases. */
    @Override
    @Transactional
    public ApiResponse complete(UUID userId, UUID videoId) {
        require(userId, videoId);
        boolean first = this.videoMapper.markCompleted(userId, videoId, Instant.now(this.clock)) == 1;
        int scheduled = 0;
        if (first) {
            List<UUID> phraseIds = this.videoMapper.phrases(videoId).stream().map(LearningDto.Phrase::id).toList();
            this.reviewService.schedule(userId, phraseIds);
            scheduled = phraseIds.size();
        }
        this.events.publishEvent(
                new PracticeRecorded(userId, PracticeRecorded.Source.VIDEO, first ? VIDEO_POINTS : 0));
        return ApiResponse.ok(new VideoDto.Completion(first, scheduled));
    }

    private VideoDto.VideoSummary require(UUID userId, UUID videoId) {
        return this.videoMapper.find(userId, videoId).orElseThrow(() -> ApiException.notFound("Video"));
    }
}
