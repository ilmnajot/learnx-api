package uz.learnx.learnxapi.service;

import java.util.UUID;
import uz.learnx.learnxapi.dto.RoleplayDto;
import uz.learnx.learnxapi.model.ApiResponse;

public interface RoleplayService {
    ApiResponse start(UUID userId, UUID lessonId);

    ApiResponse takeTurn(UUID userId, UUID sessionId, RoleplayDto.TurnRequest request);
}
