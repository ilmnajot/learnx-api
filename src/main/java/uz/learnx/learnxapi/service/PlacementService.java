package uz.learnx.learnxapi.service;

import java.util.UUID;
import uz.learnx.learnxapi.dto.PlacementDto;
import uz.learnx.learnxapi.model.ApiResponse;

public interface PlacementService {
    ApiResponse questions();

    ApiResponse submit(UUID userId, PlacementDto.SubmitRequest request);
}
