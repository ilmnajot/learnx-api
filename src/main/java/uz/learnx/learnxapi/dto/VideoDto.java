package uz.learnx.learnxapi.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class VideoDto {
    private VideoDto() {
    }

    public record VideoSummary(UUID id, String youtubeId, String title, String channel, String category,
            Integer durationSeconds, String whyWatch, String speakPrompt, Instant completedAt) {
    }

    public record VideoDetail(VideoSummary video, List<LearningDto.Phrase> phrases) {
    }

    public record Completion(boolean firstCompletion, int phrasesScheduled) {
    }
}
