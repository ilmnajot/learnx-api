package uz.learnx.learnxapi.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import uz.learnx.learnxapi.common.error.ApiException;
import uz.learnx.learnxapi.email.AccountEmails;
import uz.learnx.learnxapi.user.User;
import uz.learnx.learnxapi.user.UserMapper;
import uz.learnx.learnxapi.user.VerificationSender;

/** Email confirmation and password reset. Every link is random, stored only as a hash, and single use. */
@Service
public class AccountService implements VerificationSender {

    static final String VERIFY = "VERIFY_EMAIL";
    static final String RESET = "RESET_PASSWORD";
    static final Duration VERIFY_TTL = Duration.ofHours(48);
    static final Duration RESET_TTL = Duration.ofHours(1);

    /** Stops the app being used to flood someone's inbox, whichever IP addresses the requests come from. */
    static final int MAX_EMAILS_PER_HOUR = 3;

    private final UserMapper users;
    private final EmailTokenMapper tokens;
    private final RefreshTokenMapper refreshTokens;
    private final TokenService tokenService;
    private final AccountEmails emails;
    private final PasswordEncoder passwords;
    private final TransactionTemplate tx;
    private final Clock clock;

    AccountService(UserMapper users, EmailTokenMapper tokens, RefreshTokenMapper refreshTokens,
            TokenService tokenService, AccountEmails emails, PasswordEncoder passwords, TransactionTemplate tx,
            Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.refreshTokens = refreshTokens;
        this.tokenService = tokenService;
        this.emails = emails;
        this.passwords = passwords;
        this.tx = tx;
        this.clock = clock;
    }

    /** Quietly does nothing when the hourly limit is reached, or when the email is already confirmed. */
    @Override
    public void send(User user) {
        if (user.emailVerifiedAt() != null) {
            return;
        }
        issue(user.id(), VERIFY, VERIFY_TTL).ifIssued(token -> emails.sendVerification(user.email(), token));
    }

    public void verifyEmail(String token) {
        Instant now = Instant.now(clock);
        UUID userId = tokens.consume(tokenService.hash(token), VERIFY, now).orElseThrow(AccountService::invalidLink);
        users.markVerified(userId, now);
    }

    /** Responds the same way whether or not the account exists, so this can't be used to discover who has one. */
    public void requestPasswordReset(String email) {
        users.findByEmail(email.strip().toLowerCase(Locale.ROOT)).ifPresent(user ->
                issue(user.id(), RESET, RESET_TTL).ifIssued(token -> emails.sendPasswordReset(user.email(), token)));
    }

    /** Using the link proves control of the mailbox, so it also confirms the email and clears any lockout. */
    public void resetPassword(String token, String newPassword) {
        String hash = passwords.encode(newPassword);
        Instant now = Instant.now(clock);
        tx.executeWithoutResult(status -> {
            UUID userId = tokens.consume(tokenService.hash(token), RESET, now).orElseThrow(AccountService::invalidLink);
            users.updatePassword(userId, hash);
            users.clearFailedLogins(userId);
            users.markVerified(userId, now);
            refreshTokens.revokeAllForUser(userId, now);
        });
    }

    private Issued issue(UUID userId, String purpose, Duration ttl) {
        Instant now = Instant.now(clock);
        if (tokens.countSince(userId, purpose, now.minus(Duration.ofHours(1))) >= MAX_EMAILS_PER_HOUR) {
            return new Issued(null);
        }
        String token = tokenService.newRefreshToken();
        tx.executeWithoutResult(status -> {
            tokens.retireOpen(userId, purpose, now);
            tokens.insert(userId, purpose, tokenService.hash(token), now.plus(ttl), now);
        });
        return new Issued(token);
    }

    private record Issued(String token) {
        void ifIssued(java.util.function.Consumer<String> action) {
            if (token != null) {
                action.accept(token);
            }
        }
    }

    private static ApiException invalidLink() {
        return ApiException.badRequest("invalid_link", "This link is invalid or has expired");
    }

    @Scheduled(cron = "0 23 3 * * *")
    void purgeExpired() {
        tokens.deleteExpired(Instant.now(clock).minus(Duration.ofDays(1)));
    }
}
