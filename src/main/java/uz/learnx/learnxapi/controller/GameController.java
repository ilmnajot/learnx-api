package uz.learnx.learnxapi.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.dto.GameDto;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.security.CurrentUserId;
import uz.learnx.learnxapi.service.GameService;

@RestController
@Validated
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping("/pool")
    public ApiResponse pool(@CurrentUserId UUID userId,
            @RequestParam(defaultValue = "8") @Min(2) @Max(20) int size) {
        return this.gameService.pool(userId, size);
    }

    @PostMapping("/results")
    public ApiResponse result(@CurrentUserId UUID userId, @Valid @RequestBody GameDto.ResultRequest request) {
        return this.gameService.record(userId, request);
    }
}
