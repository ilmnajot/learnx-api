package uz.learnx.learnxapi.user;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.learnx.learnxapi.common.error.ApiException;

/** Answers "what day is it for this person?", because streaks follow the learner's own calendar. */
@Component
public class UserClock {

    private final UserMapper users;
    private final Clock clock;

    UserClock(UserMapper users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    public LocalDate today(UUID userId) {
        String timezone = users.findTimezone(userId).orElseThrow(() -> ApiException.notFound("User"));
        return LocalDate.now(clock.withZone(ZoneId.of(timezone)));
    }

    public static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
