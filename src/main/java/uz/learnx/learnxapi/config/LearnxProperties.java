package uz.learnx.learnxapi.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("learnx")
public record LearnxProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Cors cors,
        @Valid @NotNull Ai ai,
        @Valid @NotNull Mail mail,
        @Valid @NotNull RateLimit rateLimit,
        @Valid @NotNull Admin admin,
        @Valid @NotNull PublicSite publicSite) {

    public record Jwt(
            @NotBlank @Size(min = 32) String secret,
            @NotNull Duration accessTtl,
            @NotNull Duration refreshTtl,
            boolean secureCookie) {}

    public record Cors(@NotEmpty List<String> allowedOrigins) {}

    public record Ai(
            String apiKey,
            @NotBlank String model,
            @NotNull Duration timeout,
            @Min(1) int dailyRequestLimit,
            @Min(1) int globalDailyLimit) {

        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    /**
     * @param publicUrl the site's address, used in links inside emails
     * @param requireSmtp when true, startup fails unless a mail server is configured
     */
    public record Mail(@NotBlank String from, @NotBlank String publicUrl, boolean requireSmtp) {}

    public record RateLimit(
            @Min(1) int registerPerHour,
            @Min(1) int loginPerHour,
            @Min(1) int refreshPerHour,
            @Min(1) int passwordPerHour,
            @Min(1) int verifyPerHour,
            @Min(1) int aiPerHour,
            @Min(1) int commentPerHour) {}

    /** @param emails become admins once their email is confirmed */
    public record Admin(@NotNull List<String> emails) {}

    public record PublicSite(@NotNull Duration cacheTtl) {}
}
