package uz.learnx.learnxapi.service;

import java.util.UUID;
import uz.learnx.learnxapi.dto.GameDto;
import uz.learnx.learnxapi.model.ApiResponse;

public interface GameService {
    ApiResponse pool(UUID userId, int size);

    ApiResponse record(UUID userId, GameDto.ResultRequest request);
}
