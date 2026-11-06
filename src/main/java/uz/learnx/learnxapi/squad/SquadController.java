package uz.learnx.learnxapi.squad;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.common.security.CurrentUserId;

@RestController
@RequestMapping("/api/v1/squads")
class SquadController {

    record CreateRequest(@NotBlank @Size(max = 40) String name) {}

    record JoinRequest(@NotBlank @Size(max = 16) String inviteCode) {}

    private final SquadService squads;

    SquadController(SquadService squads) {
        this.squads = squads;
    }

    /** 204 means the learner has no squad yet, which is an ordinary state rather than an error. */
    @GetMapping("/me")
    ResponseEntity<SquadService.SquadView> mine(@CurrentUserId UUID userId) {
        return squads.view(userId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SquadService.SquadView create(@CurrentUserId UUID userId, @Valid @RequestBody CreateRequest request) {
        return squads.create(userId, request.name());
    }

    @PostMapping("/join")
    SquadService.SquadView join(@CurrentUserId UUID userId, @Valid @RequestBody JoinRequest request) {
        return squads.join(userId, request.inviteCode());
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void leave(@CurrentUserId UUID userId) {
        squads.leave(userId);
    }
}
