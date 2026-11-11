package uz.learnx.learnxapi.service;

import java.time.LocalDate;
import java.util.UUID;
import uz.learnx.learnxapi.dto.User;
import uz.learnx.learnxapi.dto.UserDto;
import uz.learnx.learnxapi.model.ApiResponse;

public interface UserService {
    ApiResponse me(UUID userId);

    ApiResponse updateProfile(UUID userId, UserDto.UpdateProfileRequest request);

    User getUser(UUID userId);

    /** "What day is it for this person?", because streaks follow the learner's own calendar. */
    LocalDate today(UUID userId);
}
