package uz.learnx.learnxapi.common.config;

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
public record LearnxProperties(@Valid @NotNull Jwt jwt, @Valid @NotNull Cors cors, @Valid @NotNull Ai ai) {

    public record Jwt(
            @NotBlank @Size(min = 32) String secret,
            @NotNull Duration accessTtl,
            @NotNull Duration refreshTtl,
            boolean secureCookie) {}

    public record Cors(@NotEmpty List<String> allowedOrigins) {}

    public record Ai(String apiKey, @NotBlank String model, @NotNull Duration timeout, @Min(1) int dailyRequestLimit) {

        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
