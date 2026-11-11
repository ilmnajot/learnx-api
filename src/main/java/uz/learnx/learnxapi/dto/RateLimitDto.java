package uz.learnx.learnxapi.dto;

public final class RateLimitDto {
    private RateLimitDto() {
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {
    }
}
