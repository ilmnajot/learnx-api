package uz.learnx.learnxapi.service.impl;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.learnx.learnxapi.dto.WritingDto;
import uz.learnx.learnxapi.integration.AiTutor;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.service.AiQuotaService;
import uz.learnx.learnxapi.service.UserService;
import uz.learnx.learnxapi.service.WritingService;

@Service
@RequiredArgsConstructor
public class WritingServiceImpl implements WritingService {

    private final AiTutor aiTutor;
    private final AiQuotaService aiQuotaService;
    private final UserService userService;

    @Override
    public ApiResponse improve(UUID userId, WritingDto.ImproveRequest request) {
        this.aiQuotaService.consume(userId);
        return ApiResponse.ok(this.aiTutor.improveMessage(request.text().strip(),
                this.userService.getUser(userId).nativeLanguage()));
    }
}
