package uz.learnx.learnxapi.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Signs a user out everywhere. Access tokens already issued stop working when they expire, within 15 minutes. */
@Component
public class SessionRevoker {

    private final RefreshTokenMapper refreshTokens;
    private final Clock clock;

    SessionRevoker(RefreshTokenMapper refreshTokens, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    public void revokeAll(UUID userId) {
        refreshTokens.revokeAllForUser(userId, Instant.now(clock));
    }
}
