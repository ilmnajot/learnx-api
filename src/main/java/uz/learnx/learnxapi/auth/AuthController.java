package uz.learnx.learnxapi.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.common.config.LearnxProperties;
import uz.learnx.learnxapi.user.UserView;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String COOKIE = "learnx_refresh";

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            // BCrypt only reads 72 bytes, so longer passwords are rejected rather than silently truncated.
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 80) String displayName) {}

    public record LoginRequest(@NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 72) String password) {}

    public record AuthResponse(String accessToken, UserView user) {}

    private final AuthService auth;
    private final LearnxProperties.Jwt jwt;

    AuthController(AuthService auth, LearnxProperties properties) {
        this.auth = auth;
        this.jwt = properties.jwt();
    }

    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return respond(HttpStatus.CREATED, auth.register(request.email(), request.password(), request.displayName()));
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return respond(HttpStatus.OK, auth.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(@CookieValue(name = COOKIE, required = false) String token) {
        return respond(HttpStatus.OK, auth.refresh(token));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@CookieValue(name = COOKIE, required = false) String token) {
        auth.logout(token);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    private ResponseEntity<AuthResponse> respond(HttpStatus status, AuthService.Session session) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie(session.refreshToken(), jwt.refreshTtl()).toString())
                .body(new AuthResponse(session.accessToken(), UserView.of(session.user())));
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE, value)
                .httpOnly(true)
                .secure(jwt.secureCookie())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
    }
}
