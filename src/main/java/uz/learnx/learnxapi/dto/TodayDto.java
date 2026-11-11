package uz.learnx.learnxapi.dto;

import java.time.LocalDate;
import java.util.List;

/** Read models that combine several features for the home and progress screens. */
public final class TodayDto {
    private TodayDto() {
    }

    public record TodayView(int streak, int dueReviews, LearningDto.LessonSummary nextLesson, int completedLessons,
            int totalLessons, String latestCanDo) {
    }

    public record ProgressView(int streak, int totalPracticeDays, int completedLessons, int totalLessons,
            List<String> canDos, List<LocalDate> recentDays) {
    }
}
