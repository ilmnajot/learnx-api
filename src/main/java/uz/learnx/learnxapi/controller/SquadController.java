package uz.learnx.learnxapi.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.dto.SquadDto;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.security.CurrentUserId;
import uz.learnx.learnxapi.service.SquadService;

@RestController
@RequestMapping("/api/v1/squads")
@RequiredArgsConstructor
public class SquadController {

    private final SquadService squadService;

    // no data means the learner has no squad yet
    @GetMapping("/me")
    public ApiResponse mine(@CurrentUserId UUID userId) {
        return this.squadService.mine(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse create(@CurrentUserId UUID userId, @Valid @RequestBody SquadDto.CreateRequest request) {
        return this.squadService.create(userId, request);
    }

    @PostMapping("/join")
    public ApiResponse join(@CurrentUserId UUID userId, @Valid @RequestBody SquadDto.JoinRequest request) {
        return this.squadService.join(userId, request);
    }

    @DeleteMapping("/me")
    public ApiResponse leave(@CurrentUserId UUID userId) {
        return this.squadService.leave(userId);
    }
}
