package uz.learnx.learnxapi.service;

import java.util.List;
import java.util.UUID;
import uz.learnx.learnxapi.dto.ReviewDto;
import uz.learnx.learnxapi.model.ApiResponse;

public interface ReviewService {
    ApiResponse due(UUID userId, int limit);

    ApiResponse answer(UUID userId, UUID phraseId, ReviewDto.AnswerRequest request);

    /** New phrases come back tomorrow, because the lesson itself was today's practice. */
    void schedule(UUID userId, List<UUID> phraseIds);

    int dueCount(UUID userId);

    /** A random selection for games: the learner's own phrases, topped up with starter phrases. */
    List<ReviewDto.DueReview> gamePool(UUID userId, int size);
}
