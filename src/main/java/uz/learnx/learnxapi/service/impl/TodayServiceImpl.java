package uz.learnx.learnxapi.service.impl;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.learnx.learnxapi.dto.TodayDto;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.service.ActivityService;
import uz.learnx.learnxapi.service.LearningService;
import uz.learnx.learnxapi.service.ReviewService;
import uz.learnx.learnxapi.service.TodayService;

@Service
@RequiredArgsConstructor
public class TodayServiceImpl implements TodayService {

    private static final int HEATMAP_DAYS = 28;

    private final ActivityService activityService;
    private final LearningService learningService;
    private final ReviewService reviewService;

    @Override
    public ApiResponse today(UUID userId) {
        List<String> canDos = this.learningService.completedCanDos(userId);
        return ApiResponse.ok(new TodayDto.TodayView(this.activityService.currentStreak(userId),
                this.reviewService.dueCount(userId), this.learningService.nextLesson(userId).orElse(null),
                canDos.size(), this.learningService.lessonCount(), canDos.isEmpty() ? null : canDos.getFirst()));
    }

    @Override
    public ApiResponse progress(UUID userId) {
        List<String> canDos = this.learningService.completedCanDos(userId);
        return ApiResponse.ok(new TodayDto.ProgressView(this.activityService.currentStreak(userId),
                this.activityService.totalDays(userId), canDos.size(), this.learningService.lessonCount(), canDos,
                this.activityService.recentDays(userId, HEATMAP_DAYS)));
    }
}
