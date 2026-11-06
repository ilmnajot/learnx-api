package uz.learnx.learnxapi.roleplay;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.common.security.CurrentUserId;

@RestController
@RequestMapping("/api/v1")
class RoleplayController {

    record TurnRequest(@NotBlank @Size(max = 500) String text) {}

    private final RoleplayService roleplay;

    RoleplayController(RoleplayService roleplay) {
        this.roleplay = roleplay;
    }

    @PostMapping("/lessons/{lessonId}/roleplay")
    @ResponseStatus(HttpStatus.CREATED)
    RoleplayService.Started start(@CurrentUserId UUID userId, @PathVariable UUID lessonId) {
        return roleplay.start(userId, lessonId);
    }

    @PostMapping("/roleplay/{sessionId}/turns")
    RoleplayService.TurnResult turn(@CurrentUserId UUID userId, @PathVariable UUID sessionId,
            @Valid @RequestBody TurnRequest request) {
        return roleplay.takeTurn(userId, sessionId, request.text().strip());
    }
}
