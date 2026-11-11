package uz.learnx.learnxapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class WritingDto {
    private WritingDto() {
    }

    public record ImproveRequest(@NotBlank @Size(max = 2000) String text) {
    }
}
