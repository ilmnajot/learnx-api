package uz.learnx.learnxapi.utils;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class StreakCalculator {

    private StreakCalculator() {}

    /**
     * Counts consecutive active days ending today. A streak ending yesterday is still alive,
     * because the learner has until the end of today to continue it.
     */
    public static int current(Collection<LocalDate> activeDays, LocalDate today) {
        Set<LocalDate> days = new HashSet<>(activeDays);
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
