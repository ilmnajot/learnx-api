package uz.learnx.learnxapi.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ActivityService {
    int currentStreak(UUID userId);

    List<LocalDate> recentDays(UUID userId, int days);

    int totalDays(UUID userId);
}
