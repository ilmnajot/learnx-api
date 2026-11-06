package uz.learnx.learnxapi.ai;

import java.time.LocalDate;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;
import uz.learnx.learnxapi.common.config.LearnxProperties;
import uz.learnx.learnxapi.common.error.ApiException;
import uz.learnx.learnxapi.user.UserClock;

/** Caps AI use per learner per day. The check and the increment are one SQL statement, so it holds across instances. */
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
    }

    private final AiUsageMapper usage;
    private final UserClock userClock;
    private final int dailyLimit;

    AiQuota(AiUsageMapper usage, UserClock userClock, LearnxProperties properties) {
        this.usage = usage;
        this.userClock = userClock;
        this.dailyLimit = properties.ai().dailyRequestLimit();
    }

    public void consume(UUID userId) {
        if (usage.consume(userId, userClock.today(userId), dailyLimit) == 0) {
            throw ApiException.tooManyRequests("ai_quota_exceeded", "Daily AI practice limit reached");
        }
    }
}
