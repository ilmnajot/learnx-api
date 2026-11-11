package uz.learnx.learnxapi.service;

import java.util.UUID;
import uz.learnx.learnxapi.dto.SquadDto;
import uz.learnx.learnxapi.model.ApiResponse;

public interface SquadService {
    ApiResponse mine(UUID userId);

    ApiResponse create(UUID userId, SquadDto.CreateRequest request);

    ApiResponse join(UUID userId, SquadDto.JoinRequest request);

    ApiResponse leave(UUID userId);

    /** Used when an account is deleted, so the squad keeps an owner or is removed when empty. */
    void leaveIfMember(UUID userId);
}
