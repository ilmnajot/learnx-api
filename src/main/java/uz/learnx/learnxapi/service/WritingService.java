package uz.learnx.learnxapi.service;

import java.util.UUID;
import uz.learnx.learnxapi.dto.WritingDto;
import uz.learnx.learnxapi.model.ApiResponse;

/** "Fix my message". Text is never stored or logged, because it can come from a learner's real work. */
public interface WritingService {
    ApiResponse improve(UUID userId, WritingDto.ImproveRequest request);
}
