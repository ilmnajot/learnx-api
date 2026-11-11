package uz.learnx.learnxapi.service;

import java.util.UUID;
import uz.learnx.learnxapi.model.ApiResponse;

public interface VideoService {
    ApiResponse list(UUID userId);

    ApiResponse detail(UUID userId, UUID videoId);

    ApiResponse complete(UUID userId, UUID videoId);
}
