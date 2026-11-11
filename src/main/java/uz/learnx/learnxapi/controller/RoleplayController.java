package uz.learnx.learnxapi.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.dto.RoleplayDto;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.security.CurrentUserId;
import uz.learnx.learnxapi.service.RoleplayService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RoleplayController {

    private final RoleplayService roleplayService;

    @PostMapping("/lessons/{lessonId}/roleplay")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse start(@CurrentUserId UUID userId, @PathVariable UUID lessonId) {
        return this.roleplayService.start(userId, lessonId);
    }

    // body: {"text": "..."} — what the learner said
    @PostMapping("/roleplay/{sessionId}/turns")
    public ApiResponse turn(@CurrentUserId UUID userId, @PathVariable UUID sessionId,
            @Valid @RequestBody RoleplayDto.TurnRequest request) {
        return this.roleplayService.takeTurn(userId, sessionId, request);
    }
}
