package uz.learnx.learnxapi.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.UUID;

public final class UserDto {
    private UserDto() {
    }

    /** What the learner studies. English learners get the full course; Arabic learners start with the alphabet. */
    public enum Course {
        ENGLISH,
        ARABIC_LETTERS
    }

    /** The public shape of a user. Never includes credentials or lockout state. */
    public record UserView(
            UUID id,
            String email,
            String displayName,
            String jobRole,
            String goal,
            String level,
            String nativeLanguage,
            String timezone,
            LocalTime reminderTime,
            boolean onboarded,
            boolean emailVerified,
            boolean admin,
            String course
    ) {
        public static UserView of(User u) {
            return new UserView(u.id(), u.email(), u.displayName(), u.jobRole(), u.goal(), u.level(),
                    u.nativeLanguage(), u.timezone(), u.reminderTime(), u.onboardedAt() != null,
                    u.emailVerifiedAt() != null, u.isAdmin(), u.course());
        }
    }

    public record UpdateProfileRequest(
            @Size(min = 1, max = 80) String displayName,
            @Size(max = 80) String jobRole,
            @Size(max = 200) String goal,
            @Size(max = 64) String timezone,
            LocalTime reminderTime,
            Boolean onboarded,
            Course course
    ) {
    }
}
