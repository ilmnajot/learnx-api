package uz.learnx.learnxapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class AuthDto {
    private AuthDto() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            // BCrypt only reads 72 bytes, so longer passwords are rejected rather than silently truncated.
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 80) String displayName
    ) {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password
    ) {
    }

    public record TokenRequest(@NotBlank @Size(max = 128) String token) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 254) String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Size(max = 128) String token,
            @NotBlank @Size(min = 8, max = 72) String password
    ) {
    }

    public record AuthResponse(String accessToken, UserDto.UserView user) {
    }

    /** A signed-in session. The refresh token only ever leaves the API inside an HttpOnly cookie. */
    public record Session(User user, String accessToken, String refreshToken) {
    }

    public record RefreshToken(UUID id, UUID userId, Instant expiresAt, Instant revokedAt, Instant rotatedAt) {
    }
}
