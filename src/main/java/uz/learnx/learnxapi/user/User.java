package uz.learnx.learnxapi.user;

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
        Instant emailVerifiedAt) {}
