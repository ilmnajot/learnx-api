package uz.learnx.learnxapi.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import uz.learnx.learnxapi.dto.AuthDto;
import uz.learnx.learnxapi.dto.User;
import uz.learnx.learnxapi.exception.ApiException;
import uz.learnx.learnxapi.mapper.EmailTokenMapper;
import uz.learnx.learnxapi.mapper.RefreshTokenMapper;
import uz.learnx.learnxapi.mapper.UserMapper;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.service.AccountService;
import uz.learnx.learnxapi.service.AdminPromotionService;
import uz.learnx.learnxapi.service.EmailService;
import uz.learnx.learnxapi.service.TokenService;

/** Every link is random, stored only as a hash, and single use. */
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    static final String VERIFY = "VERIFY_EMAIL";
    static final String RESET = "RESET_PASSWORD";
    static final Duration VERIFY_TTL = Duration.ofHours(48);
    static final Duration RESET_TTL = Duration.ofHours(1);

    /** Stops the app being used to flood someone's inbox, whichever IP addresses the requests come from. */
    static final int MAX_EMAILS_PER_HOUR = 3;

    private final UserMapper userMapper;
    private final EmailTokenMapper emailTokenMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate tx;
    private final AdminPromotionService adminPromotionService;
    private final Clock clock;

    /** Quietly does nothing when the hourly limit is reached, or when the email is already confirmed. */
    @Override
    public void sendVerification(User user) {
        if (user.emailVerifiedAt() != null) {
            return;
        }
        issue(user.id(), VERIFY, VERIFY_TTL).ifIssued(token -> this.emailService.sendVerification(user.email(), token));
    }

    @Override
    public ApiResponse resendVerification(UUID userId) {
        sendVerification(this.userMapper.findById(userId).orElseThrow(() -> ApiException.notFound("User")));
        return ApiResponse.accepted();
    }

    @Override
    public ApiResponse verifyEmail(AuthDto.TokenRequest request) {
        Instant now = Instant.now(this.clock);
        UUID userId = this.emailTokenMapper.consume(this.tokenService.hash(request.token()), VERIFY, now)
                .orElseThrow(AccountServiceImpl::invalidLink);
        this.userMapper.markVerified(userId, now);
        this.adminPromotionService.promoteConfigured();
        return ApiResponse.ok();
    }

    /** Responds the same way whether or not the account exists, so this can't be used to discover who has one. */
    @Override
    public ApiResponse forgotPassword(AuthDto.ForgotPasswordRequest request) {
        this.userMapper.findByEmail(request.email().strip().toLowerCase(Locale.ROOT)).ifPresent(user ->
                issue(user.id(), RESET, RESET_TTL)
                        .ifIssued(token -> this.emailService.sendPasswordReset(user.email(), token)));
        return ApiResponse.accepted();
    }

    /** Using the link proves control of the mailbox, so it also confirms the email and clears any lockout. */
    @Override
    public ApiResponse resetPassword(AuthDto.ResetPasswordRequest request) {
        String hash = this.passwordEncoder.encode(request.password());
        Instant now = Instant.now(this.clock);
        this.tx.executeWithoutResult(status -> {
            UUID userId = this.emailTokenMapper.consume(this.tokenService.hash(request.token()), RESET, now)
                    .orElseThrow(AccountServiceImpl::invalidLink);
            this.userMapper.updatePassword(userId, hash);
            this.userMapper.clearFailedLogins(userId);
            this.userMapper.markVerified(userId, now);
            this.refreshTokenMapper.revokeAllForUser(userId, now);
            this.adminPromotionService.promoteConfigured();
        });
        return ApiResponse.ok();
    }

    @Scheduled(cron = "0 23 3 * * *")
    public void purgeExpired() {
        this.emailTokenMapper.deleteExpired(Instant.now(this.clock).minus(Duration.ofDays(1)));
    }

    private Issued issue(UUID userId, String purpose, Duration ttl) {
        Instant now = Instant.now(this.clock);
        if (this.emailTokenMapper.countSince(userId, purpose, now.minus(Duration.ofHours(1))) >= MAX_EMAILS_PER_HOUR) {
            return new Issued(null);
        }
        String token = this.tokenService.newRandomToken();
        this.tx.executeWithoutResult(status -> {
            this.emailTokenMapper.retireOpen(userId, purpose, now);
            this.emailTokenMapper.insert(userId, purpose, this.tokenService.hash(token), now.plus(ttl), now);
        });
        return new Issued(token);
    }

    private record Issued(String token) {
        void ifIssued(Consumer<String> action) {
            if (token != null) {
                action.accept(token);
            }
        }
    }

    private static ApiException invalidLink() {
        return ApiException.badRequest("invalid_link", "This link is invalid or has expired");
    }
}
