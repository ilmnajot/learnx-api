package uz.learnx.learnxapi.controller;

import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.learnx.learnxapi.config.LearnxProperties;
import uz.learnx.learnxapi.dto.AuthDto;
import uz.learnx.learnxapi.dto.UserDto;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.service.AccountService;
import uz.learnx.learnxapi.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String COOKIE = "learnx_refresh";

    private final AuthService authService;
    private final AccountService accountService;
    private final LearnxProperties properties;

    // public — signs the new learner in and emails a confirmation link
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        AuthDto.Session session = this.authService.register(request);
        this.accountService.sendVerification(session.user());
        return respond(HttpStatus.CREATED, session);
    }

    // public
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        return respond(HttpStatus.OK, this.authService.login(request));
    }

    // public — the refresh token comes from the HttpOnly cookie
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refresh(@CookieValue(name = COOKIE, required = false) String token) {
        return respond(HttpStatus.OK, this.authService.refresh(token));
    }

    // public
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@CookieValue(name = COOKIE, required = false) String token) {
        this.authService.logout(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .body(ApiResponse.ok());
    }

    // public — body: {"token": "..."} from the emailed link
    @PostMapping("/verify-email")
    public ApiResponse verifyEmail(@Valid @RequestBody AuthDto.TokenRequest request) {
        return this.accountService.verifyEmail(request);
    }

    // public — always 202, so it can't reveal which emails have an account
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse forgotPassword(@Valid @RequestBody AuthDto.ForgotPasswordRequest request) {
        return this.accountService.forgotPassword(request);
    }

    // public — body: {"token": "...", "password": "..."}
    @PostMapping("/reset-password")
    public ApiResponse resetPassword(@Valid @RequestBody AuthDto.ResetPasswordRequest request) {
        return this.accountService.resetPassword(request);
    }

    private ResponseEntity<ApiResponse> respond(HttpStatus status, AuthDto.Session session) {
        AuthDto.AuthResponse body = new AuthDto.AuthResponse(session.accessToken(), UserDto.UserView.of(session.user()));
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE,
                        cookie(session.refreshToken(), this.properties.jwt().refreshTtl()).toString())
                .body(ApiResponse.builder().status(status).message("success").data(body).build());
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE, value)
                .httpOnly(true)
                .secure(this.properties.jwt().secureCookie())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
    }
}
