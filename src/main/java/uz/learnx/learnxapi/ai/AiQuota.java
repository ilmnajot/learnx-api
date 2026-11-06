package uz.learnx.learnxapi.ai;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uz.learnx.learnxapi.common.config.LearnxProperties;
import uz.learnx.learnxapi.common.error.ApiException;
import uz.learnx.learnxapi.user.User;
import uz.learnx.learnxapi.user.UserClock;
import uz.learnx.learnxapi.user.UserService;

/**
 * Guards AI spending with three checks: a confirmed email (so throwaway accounts can't use AI), a per-learner daily
 * limit, and a daily ceiling across everyone. Each limit is one atomic SQL statement, so it holds across instances.
 */
@Component
public class AiQuota {

    @Mapper
    public interface AiUsageMapper {
        @Update("""
                INSERT INTO ai_usage (user_id, usage_date, requests) VALUES (#{userId}, #{date}, 1)
                ON CONFLICT (user_id, usage_date)
                DO UPDATE SET requests = ai_usage.requests + 1 WHERE ai_usage.requests < #{limit}
                """)
        int consume(@Param("userId") UUID userId, @Param("date") LocalDate date, @Param("limit") int limit);

        @Insert("""
                INSERT INTO ai_global_usage (usage_date, requests) VALUES (#{date}, 1)
                ON CONFLICT (usage_date)
                DO UPDATE SET requests = ai_global_usage.requests + 1 WHERE ai_global_usage.requests < #{limit}
                """)
        int consumeGlobal(@Param("date") LocalDate date, @Param("limit") int limit);
    }

    private final AiUsageMapper usage;
    private final UserClock userClock;
    private final UserService users;
    private final Clock clock;
    private final int dailyLimit;
    private final int globalDailyLimit;

    AiQuota(AiUsageMapper usage, UserClock userClock, UserService users, Clock clock, LearnxProperties properties) {
        this.usage = usage;
        this.userClock = userClock;
        this.users = users;
        this.clock = clock;
        this.dailyLimit = properties.ai().dailyRequestLimit();
        this.globalDailyLimit = properties.ai().globalDailyLimit();
    }

    public void consume(UUID userId) {
        User user = users.get(userId);
        if (user.bannedAt() != null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "account_banned", "This account has been blocked");
        }
        if (user.emailVerifiedAt() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "email_not_verified", "Confirm your email to use AI practice");
        }
        if (usage.consume(userId, userClock.today(userId), dailyLimit) == 0) {
            throw ApiException.tooManyRequests("ai_quota_exceeded", "Daily AI practice limit reached");
        }
        if (usage.consumeGlobal(LocalDate.now(clock.withZone(ZoneOffset.UTC)), globalDailyLimit) == 0) {
            throw ApiException.tooManyRequests("ai_capacity_reached", "AI practice is at capacity today");
        }
    }
}
