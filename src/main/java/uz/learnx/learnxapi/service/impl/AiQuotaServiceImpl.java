package uz.learnx.learnxapi.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.learnx.learnxapi.config.LearnxProperties;
import uz.learnx.learnxapi.dto.User;
import uz.learnx.learnxapi.exception.ApiException;
import uz.learnx.learnxapi.mapper.AiUsageMapper;
import uz.learnx.learnxapi.service.AiQuotaService;
import uz.learnx.learnxapi.service.UserService;

@Service
@RequiredArgsConstructor
public class AiQuotaServiceImpl implements AiQuotaService {

    private final AiUsageMapper aiUsageMapper;
    private final UserService userService;
    private final LearnxProperties properties;
    private final Clock clock;

    @Override
    public void consume(UUID userId) {
        User user = this.userService.getUser(userId);
        if (user.bannedAt() != null) {
            throw ApiException.forbidden("account_banned", "This account has been blocked");
        }
        if (user.emailVerifiedAt() == null) {
            throw ApiException.forbidden("email_not_verified", "Confirm your email to use AI practice");
        }
        LearnxProperties.Ai ai = this.properties.ai();
        if (this.aiUsageMapper.consume(userId, this.userService.today(userId), ai.dailyRequestLimit()) == 0) {
            throw ApiException.tooManyRequests("ai_quota_exceeded", "Daily AI practice limit reached");
        }
        LocalDate utcToday = LocalDate.now(this.clock.withZone(ZoneOffset.UTC));
        if (this.aiUsageMapper.consumeGlobal(utcToday, ai.globalDailyLimit()) == 0) {
            throw ApiException.tooManyRequests("ai_capacity_reached", "AI practice is at capacity today");
        }
    }
}
