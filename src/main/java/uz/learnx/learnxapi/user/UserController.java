package uz.learnx.learnxapi.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.common.security.CurrentUserId;
import uz.learnx.learnxapi.user.UserService.ProfileChanges;

@RestController
@RequestMapping("/api/v1/me")
class UserController {

    record UpdateProfileRequest(
            @Size(min = 1, max = 80) String displayName,
            @Size(max = 80) String jobRole,
            @Size(max = 200) String goal,
            @Size(max = 64) String timezone,
            LocalTime reminderTime,
            Boolean onboarded) {}

    private final UserService users;
    private final VerificationSender verification;

    UserController(UserService users, VerificationSender verification) {
        this.users = users;
        this.verification = verification;
    }

    @GetMapping
    UserView me(@CurrentUserId UUID userId) {
        return UserView.of(users.get(userId));
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void resendVerification(@CurrentUserId UUID userId) {
        verification.send(users.get(userId));
    }

    @PatchMapping
    UserView update(@CurrentUserId UUID userId, @Valid @RequestBody UpdateProfileRequest r) {
        return UserView.of(users.update(userId, new ProfileChanges(r.displayName(), r.jobRole(), r.goal(),
                r.timezone(), r.reminderTime(), Boolean.TRUE.equals(r.onboarded()))));
    }
}
