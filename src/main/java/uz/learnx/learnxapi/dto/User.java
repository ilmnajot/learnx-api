package uz.learnx.learnxapi.dto;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record User(
        UUID id,
        String email,
        String passwordHash,
        String displayName,
        String jobRole,
        String goal,
        String level,
        String nativeLanguage,
        String timezone,
        LocalTime reminderTime,
        Instant onboardedAt,
        Integer failedLoginAttempts,
        Instant lockedUntil,
        Instant emailVerifiedAt,
        String role,
        Instant bannedAt,
        String course) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean learnsArabicLetters() {
        return UserDto.Course.ARABIC_LETTERS.name().equals(course);
    }
}
