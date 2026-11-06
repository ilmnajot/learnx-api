package uz.learnx.learnxapi.user;

import java.time.LocalTime;
import java.util.UUID;

/** The public shape of a user. Never includes credentials or lockout state. */
public record UserView(UUID id, String email, String displayName, String jobRole, String goal, String level,
        String nativeLanguage, String timezone, LocalTime reminderTime, boolean onboarded, boolean emailVerified,
        boolean admin) {

    public static UserView of(User u) {
        return new UserView(u.id(), u.email(), u.displayName(), u.jobRole(), u.goal(), u.level(), u.nativeLanguage(),
                u.timezone(), u.reminderTime(), u.onboardedAt() != null, u.emailVerifiedAt() != null, u.isAdmin());
    }
}
