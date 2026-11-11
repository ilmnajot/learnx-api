package uz.learnx.learnxapi.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import uz.learnx.learnxapi.event.PracticeRecorded;
import uz.learnx.learnxapi.mapper.ActivityMapper;
import uz.learnx.learnxapi.service.ActivityService;
import uz.learnx.learnxapi.service.UserService;
import uz.learnx.learnxapi.utils.StreakCalculator;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private static final int STREAK_LOOKBACK_DAYS = 730;

    private final ActivityMapper activityMapper;
    private final UserService userService;

    @EventListener
    public void on(PracticeRecorded event) {
        this.activityMapper.record(event.userId(), this.userService.today(event.userId()), event.points());
    }

    @Override
    public int currentStreak(UUID userId) {
        LocalDate today = this.userService.today(userId);
        return StreakCalculator.current(
                this.activityMapper.activeDays(userId, today.minusDays(STREAK_LOOKBACK_DAYS)), today);
    }

    @Override
    public List<LocalDate> recentDays(UUID userId, int days) {
        return this.activityMapper.activeDays(userId, this.userService.today(userId).minusDays(days - 1L));
    }

    @Override
    public int totalDays(UUID userId) {
        return this.activityMapper.totalDays(userId);
    }
}
