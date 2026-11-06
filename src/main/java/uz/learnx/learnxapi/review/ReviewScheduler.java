package uz.learnx.learnxapi.review;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A simplified SM-2 spaced repetition schedule. Remembered phrases return after growing gaps
 * (1, 3, 7 days, then multiplied by ease); a miss brings the phrase back tomorrow and lowers its ease.
 */
public final class ReviewScheduler {

    private static final BigDecimal MIN_EASE = new BigDecimal("1.30");
    private static final BigDecimal MAX_EASE = new BigDecimal("3.00");
    private static final BigDecimal EASE_GAIN = new BigDecimal("0.10");
    private static final BigDecimal EASE_LOSS = new BigDecimal("0.20");
    private static final int MAX_INTERVAL_DAYS = 180;

    public record State(BigDecimal ease, int intervalDays, int repetitions, int lapses) {}

    private ReviewScheduler() {}

    public static State next(State current, boolean remembered) {
        if (!remembered) {
            return new State(current.ease().subtract(EASE_LOSS).max(MIN_EASE), 1, 0, current.lapses() + 1);
        }
        int repetitions = current.repetitions() + 1;
        int interval = switch (repetitions) {
            case 1 -> 1;
            case 2 -> 3;
            case 3 -> 7;
            default -> BigDecimal.valueOf(current.intervalDays())
                    .multiply(current.ease())
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
        };
        return new State(current.ease().add(EASE_GAIN).min(MAX_EASE), Math.min(interval, MAX_INTERVAL_DAYS),
                repetitions, current.lapses());
    }
}
