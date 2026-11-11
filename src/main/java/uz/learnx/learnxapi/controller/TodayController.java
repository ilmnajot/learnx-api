package uz.learnx.learnxapi.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.security.CurrentUserId;
import uz.learnx.learnxapi.service.TodayService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TodayController {

    private final TodayService todayService;

    // the home screen
    @GetMapping("/today")
    public ApiResponse today(@CurrentUserId UUID userId) {
        return this.todayService.today(userId);
    }

    // the progress screen
    @GetMapping("/progress")
    public ApiResponse progress(@CurrentUserId UUID userId) {
        return this.todayService.progress(userId);
    }
}
