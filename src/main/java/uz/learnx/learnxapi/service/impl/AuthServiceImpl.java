package uz.learnx.learnxapi.service.impl;

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
import uz.learnx.learnxapi.dto.AuthDto;
import uz.learnx.learnxapi.dto.User;
import uz.learnx.learnxapi.exception.ApiException;
import uz.learnx.learnxapi.mapper.RefreshTokenMapper;
import uz.learnx.learnxapi.mapper.UserMapper;
import uz.learnx.learnxapi.service.AuthService;
import uz.learnx.learnxapi.service.TokenService;

@Service
public class AuthServiceImpl implements AuthService {

    static final int MAX_FAILED_LOGINS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    /**
     * How long a just-rotated token still works. A reload can cancel a refresh after the server has rotated
     * the token but before the browser saves the new one, and that must not look like theft.
     */
    static final Duration ROTATION_GRACE = Duration.ofSeconds(10);

    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final String dummyHash;

    public AuthServiceImpl(UserMapper userMapper, RefreshTokenMapper refreshTokenMapper, TokenService tokenService,
            PasswordEncoder passwordEncoder, TransactionTemplate tx, Clock clock) {
        this.userMapper = userMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.tx = tx;
        this.clock = clock;
        // Checked when the email is unknown, so response time does not reveal which emails exist.
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    public AuthDto.Session register(AuthDto.RegisterRequest request) {
        String email = normalize(request.email());
        String hash = this.passwordEncoder.encode(request.password());
        UUID id = UUID.randomUUID();
        try {
            this.userMapper.insert(id, email, hash, request.displayName().strip());
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("email_taken", "This email is already registered");
        }
        return openSession(this.userMapper.findById(id).orElseThrow());
    }

    @Override
    public AuthDto.Session login(AuthDto.LoginRequest request) {
        Optional<User> found = this.userMapper.findByEmail(normalize(request.email()));
        if (found.isEmpty()) {
            this.passwordEncoder.matches(request.password(), this.dummyHash);
            throw invalidCredentials();
        }
        User user = found.get();
        Instant now = Instant.now(this.clock);
        if (user.lockedUntil() != null && user.lockedUntil().isAfter(now)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "account_locked",
                    "Too many failed attempts. Try again later");
        }
        if (!this.passwordEncoder.matches(request.password(), user.passwordHash())) {
            this.userMapper.recordFailedLogin(user.id(), MAX_FAILED_LOGINS, now.plus(LOCK_DURATION));
            throw invalidCredentials();
        }
        if (user.bannedAt() != null) {
            throw ApiException.forbidden("account_banned", "This account has been blocked");
        }
        this.userMapper.clearFailedLogins(user.id());
        return openSession(user);
    }

    /**
     * Rotates the token. Presenting a revoked token signals theft, so every session of that user is revoked,
     * unless it was rotated within {@link #ROTATION_GRACE}.
     */
    @Override
    public AuthDto.Session refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidSession();
        }
        Instant now = Instant.now(this.clock);
        // The reuse branch must commit its revocations, so it returns a result rather than throwing inside the tx.
        Optional<AuthDto.Session> session = this.tx.execute(status -> {
            AuthDto.RefreshToken token = this.refreshTokenMapper.findForUpdate(this.tokenService.hash(rawToken))
                    .orElseThrow(this::invalidSession);
            if (token.revokedAt() != null) {
                if (token.rotatedAt() != null && token.rotatedAt().plus(ROTATION_GRACE).isAfter(now)) {
                    return Optional.of(openSession(activeUser(token.userId())));
                }
                this.refreshTokenMapper.revokeAllForUser(token.userId(), now);
                return Optional.<AuthDto.Session>empty();
            }
            if (token.expiresAt().isBefore(now)) {
                throw invalidSession();
            }
            User user = activeUser(token.userId());
            this.refreshTokenMapper.rotate(token.id(), now);
            return Optional.of(openSession(user));
        });
        return session.orElseThrow(this::invalidSession);
    }

    @Override
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        Instant now = Instant.now(this.clock);
        this.tx.executeWithoutResult(status -> this.refreshTokenMapper
                .findForUpdate(this.tokenService.hash(rawToken))
                .ifPresent(token -> this.refreshTokenMapper.revoke(token.id(), now)));
    }

    @Override
    public void revokeAllSessions(UUID userId) {
        this.refreshTokenMapper.revokeAllForUser(userId, Instant.now(this.clock));
    }

    @Scheduled(cron = "0 17 3 * * *")
    public void purgeExpiredRefreshTokens() {
        this.refreshTokenMapper.deleteExpired(Instant.now(this.clock));
    }

    private User activeUser(UUID userId) {
        User user = this.userMapper.findById(userId).orElseThrow(this::invalidSession);
        if (user.bannedAt() != null) {
            throw invalidSession();
        }
        return user;
    }

    private AuthDto.Session openSession(User user) {
        String refresh = this.tokenService.newRandomToken();
        this.refreshTokenMapper.insert(user.id(), this.tokenService.hash(refresh), this.tokenService.refreshExpiry());
        return new AuthDto.Session(user, this.tokenService.accessToken(user.id()), refresh);
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
