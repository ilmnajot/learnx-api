package uz.learnx.learnxapi.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import uz.learnx.learnxapi.auth.RefreshTokenMapper.RefreshToken;
import uz.learnx.learnxapi.common.error.ApiException;
import uz.learnx.learnxapi.user.User;
import uz.learnx.learnxapi.user.UserMapper;

@Service
public class AuthService {

    static final int MAX_FAILED_LOGINS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    /**
     * How long a just-rotated token still works. A reload can cancel a refresh after the server has rotated
     * the token but before the browser saves the new one, and that must not look like theft.
     */
    static final Duration ROTATION_GRACE = Duration.ofSeconds(10);

    public record Session(User user, String accessToken, String refreshToken) {}

    private final UserMapper users;
    private final RefreshTokenMapper refreshTokens;
    private final TokenService tokens;
    private final PasswordEncoder passwords;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final String dummyHash;

    AuthService(UserMapper users, RefreshTokenMapper refreshTokens, TokenService tokens, PasswordEncoder passwords,
            TransactionTemplate tx, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.tokens = tokens;
        this.passwords = passwords;
        this.tx = tx;
        this.clock = clock;
        // Checked when the email is unknown, so response time does not reveal which emails exist.
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    public Session register(String email, String password, String displayName) {
        String normalized = normalize(email);
        String hash = passwords.encode(password);
        UUID id = UUID.randomUUID();
        try {
            users.insert(id, normalized, hash, displayName.strip());
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("email_taken", "This email is already registered");
        }
        return openSession(users.findById(id).orElseThrow());
    }

    public Session login(String email, String password) {
        Optional<User> found = users.findByEmail(normalize(email));
        if (found.isEmpty()) {
            passwords.matches(password, dummyHash);
            throw invalidCredentials();
        }
        User user = found.get();
        Instant now = Instant.now(clock);
        if (user.lockedUntil() != null && user.lockedUntil().isAfter(now)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "account_locked",
                    "Too many failed attempts. Try again later");
        }
        if (!passwords.matches(password, user.passwordHash())) {
            users.recordFailedLogin(user.id(), MAX_FAILED_LOGINS, now.plus(LOCK_DURATION));
            throw invalidCredentials();
        }
        users.clearFailedLogins(user.id());
        return openSession(user);
    }

    /**
     * Rotates the token. Presenting a revoked token signals theft, so every session of that user is revoked,
     * unless it was rotated within {@link #ROTATION_GRACE}.
     */
    public Session refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidSession();
        }
        Instant now = Instant.now(clock);
        // The reuse branch must commit its revocations, so it returns a result rather than throwing inside the tx.
        Optional<Session> session = tx.execute(status -> {
            RefreshToken token = refreshTokens.findForUpdate(tokens.hash(rawToken)).orElseThrow(this::invalidSession);
            if (token.revokedAt() != null) {
                if (token.rotatedAt() != null && token.rotatedAt().plus(ROTATION_GRACE).isAfter(now)) {
                    return Optional.of(openSession(users.findById(token.userId()).orElseThrow(this::invalidSession)));
                }
                refreshTokens.revokeAllForUser(token.userId(), now);
                return Optional.<Session>empty();
            }
            if (token.expiresAt().isBefore(now)) {
                throw invalidSession();
            }
            refreshTokens.rotate(token.id(), now);
            return Optional.of(openSession(users.findById(token.userId()).orElseThrow(this::invalidSession)));
        });
        return session.orElseThrow(this::invalidSession);
    }

    @Scheduled(cron = "0 17 3 * * *")
    void purgeExpiredRefreshTokens() {
        refreshTokens.deleteExpired(Instant.now(clock));
    }

    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        Instant now = Instant.now(clock);
        tx.executeWithoutResult(status -> refreshTokens
                .findForUpdate(tokens.hash(rawToken))
                .ifPresent(token -> refreshTokens.revoke(token.id(), now)));
    }

    private Session openSession(User user) {
        String refresh = tokens.newRefreshToken();
        refreshTokens.insert(user.id(), tokens.hash(refresh), tokens.refreshExpiry());
        return new Session(user, tokens.accessToken(user.id()), refresh);
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private ApiException invalidCredentials() {
        return ApiException.unauthorized("invalid_credentials", "Email or password is incorrect");
    }

    private ApiException invalidSession() {
        return ApiException.unauthorized("invalid_session", "Sign in again");
    }
}
