package uz.learnx.learnxapi.today;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.activity.ActivityService;
import uz.learnx.learnxapi.common.security.CurrentUserId;
import uz.learnx.learnxapi.learning.LearningMapper.LessonSummary;
import uz.learnx.learnxapi.learning.LearningService;
import uz.learnx.learnxapi.review.ReviewService;

/** Read models that combine several features for the home and progress screens. */
@RestController
@RequestMapping("/api/v1")
class TodayController {

    private static final int HEATMAP_DAYS = 28;

    record TodayView(int streak, int dueReviews, LessonSummary nextLesson, int completedLessons, int totalLessons,
            String latestCanDo) {}

    record ProgressView(int streak, int totalPracticeDays, int completedLessons, int totalLessons,
            List<String> canDos, List<LocalDate> recentDays) {}

    private final ActivityService activity;
    private final LearningService learning;
    private final ReviewService reviews;

    TodayController(ActivityService activity, LearningService learning, ReviewService reviews) {
        this.activity = activity;
        this.learning = learning;
        this.reviews = reviews;
    }

    @GetMapping("/today")
    TodayView today(@CurrentUserId UUID userId) {
        List<String> canDos = learning.completedCanDos(userId);
        return new TodayView(activity.currentStreak(userId), reviews.dueCount(userId),
                learning.nextLesson(userId).orElse(null), canDos.size(), learning.lessonCount(),
                canDos.isEmpty() ? null : canDos.getFirst());
    }

    @GetMapping("/progress")
    ProgressView progress(@CurrentUserId UUID userId) {
        List<String> canDos = learning.completedCanDos(userId);
        return new ProgressView(activity.currentStreak(userId), activity.totalDays(userId), canDos.size(),
                learning.lessonCount(), canDos, activity.recentDays(userId, HEATMAP_DAYS));
    }
}
