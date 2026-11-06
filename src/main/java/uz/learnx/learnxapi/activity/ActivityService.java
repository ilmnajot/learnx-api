package uz.learnx.learnxapi.activity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import uz.learnx.learnxapi.user.UserClock;

@Service
public class ActivityService {

    private static final int STREAK_LOOKBACK_DAYS = 730;

    private final ActivityMapper activity;
    private final UserClock userClock;

    ActivityService(ActivityMapper activity, UserClock userClock) {
        this.activity = activity;
        this.userClock = userClock;
    }

    @EventListener
    void on(PracticeRecorded event) {
        activity.record(event.userId(), userClock.today(event.userId()), event.points());
    }

    public int currentStreak(UUID userId) {
        LocalDate today = userClock.today(userId);
        return StreakCalculator.current(activity.activeDays(userId, today.minusDays(STREAK_LOOKBACK_DAYS)), today);
    }

    public List<LocalDate> recentDays(UUID userId, int days) {
        return activity.activeDays(userId, userClock.today(userId).minusDays(days - 1L));
    }

    public int totalDays(UUID userId) {
        return activity.totalDays(userId);
    }
}
