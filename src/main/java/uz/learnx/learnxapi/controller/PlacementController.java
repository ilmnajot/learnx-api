package uz.learnx.learnxapi.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.dto.PlacementDto;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.security.CurrentUserId;
import uz.learnx.learnxapi.service.PlacementService;

@RestController
@RequestMapping("/api/v1/placement")
@RequiredArgsConstructor
public class PlacementController {

    private final PlacementService placementService;

    // the questions, without their correct answers
    @GetMapping("/questions")
    public ApiResponse questions() {
        return this.placementService.questions();
    }

    // sets the learner's level from their answers
    @PostMapping("/submit")
    public ApiResponse submit(@CurrentUserId UUID userId, @Valid @RequestBody PlacementDto.SubmitRequest request) {
        return this.placementService.submit(userId, request);
    }
}
