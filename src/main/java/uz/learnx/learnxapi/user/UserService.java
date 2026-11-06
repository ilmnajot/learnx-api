package uz.learnx.learnxapi.user;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.learnx.learnxapi.common.error.ApiException;

@Service
public class UserService {

    public record ProfileChanges(String displayName, String jobRole, String goal, String timezone,
            LocalTime reminderTime, boolean completeOnboarding) {}

    private final UserMapper users;
    private final Clock clock;

    UserService(UserMapper users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public User get(UUID userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
    }

    @Transactional
    public User update(UUID userId, ProfileChanges changes) {
        validateTimezone(changes.timezone());
        users.updateProfile(userId, changes.displayName(), changes.jobRole(), changes.goal(), changes.timezone(),
                changes.reminderTime());
        if (changes.completeOnboarding()) {
            users.markOnboarded(userId, Instant.now(clock));
        }
        return get(userId);
    }

    private static void validateTimezone(String timezone) {
        if (timezone == null) {
            return;
        }
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw ApiException.badRequest("invalid_timezone", "Unknown timezone");
        }
    }
}
