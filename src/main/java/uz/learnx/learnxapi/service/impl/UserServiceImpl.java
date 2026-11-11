package uz.learnx.learnxapi.service.impl;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.learnx.learnxapi.dto.User;
import uz.learnx.learnxapi.dto.UserDto;
import uz.learnx.learnxapi.exception.ApiException;
import uz.learnx.learnxapi.mapper.UserMapper;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.service.UserService;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse me(UUID userId) {
        return ApiResponse.ok(UserDto.UserView.of(getUser(userId)));
    }

    @Override
    @Transactional
    public ApiResponse updateProfile(UUID userId, UserDto.UpdateProfileRequest r) {
        validateTimezone(r.timezone());
        this.userMapper.updateProfile(userId, r.displayName(), r.jobRole(), r.goal(), r.timezone(), r.reminderTime(),
                r.course() == null ? null : r.course().name());
        if (Boolean.TRUE.equals(r.onboarded())) {
            this.userMapper.markOnboarded(userId, Instant.now(this.clock));
        }
        return ApiResponse.ok(UserDto.UserView.of(getUser(userId)));
    }

    @Override
    public User getUser(UUID userId) {
        return this.userMapper.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
    }

    @Override
    public LocalDate today(UUID userId) {
        String timezone = this.userMapper.findTimezone(userId).orElseThrow(() -> ApiException.notFound("User"));
        return LocalDate.now(this.clock.withZone(ZoneId.of(timezone)));
    }

    private static void validateTimezone(String timezone) {
        if (timezone == null) {
            return;
        }
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw ApiException.badRequest("invalid_timezone", "Unknown timezone");
        }
    }
}
