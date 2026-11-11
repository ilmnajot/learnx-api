package uz.learnx.learnxapi.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.dto.WritingDto;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.security.CurrentUserId;
import uz.learnx.learnxapi.service.WritingService;

@RestController
@RequestMapping("/api/v1/writing")
@RequiredArgsConstructor
public class WritingController {

    private final WritingService writingService;

    // body: {"text": "..."} — never stored or logged
    @PostMapping("/improve")
    public ApiResponse improve(@CurrentUserId UUID userId, @Valid @RequestBody WritingDto.ImproveRequest request) {
        return this.writingService.improve(userId, request);
    }
}
