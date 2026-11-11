package uz.learnx.learnxapi.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import uz.learnx.learnxapi.dto.ReviewDto;

@Mapper
public interface ReviewMapper {

    void schedule(@Param("userId") UUID userId, @Param("phraseIds") List<UUID> phraseIds,
            @Param("dueAt") Instant dueAt);

    List<ReviewDto.DueReview> due(@Param("userId") UUID userId, @Param("now") Instant now, @Param("limit") int limit);

    int dueCount(@Param("userId") UUID userId, @Param("now") Instant now);

    Optional<ReviewDto.ReviewRow> findForUpdate(@Param("userId") UUID userId, @Param("phraseId") UUID phraseId);

    void update(@Param("userId") UUID userId, @Param("phraseId") UUID phraseId, @Param("ease") BigDecimal ease,
            @Param("intervalDays") int intervalDays, @Param("repetitions") int repetitions,
            @Param("lapses") int lapses, @Param("dueAt") Instant dueAt, @Param("now") Instant now);

    /** Phrases the learner has met, in random order, for games. */
    List<ReviewDto.DueReview> learnedSample(@Param("userId") UUID userId, @Param("limit") int limit);

    /** Starter phrases for learners with too few of their own. */
    List<ReviewDto.DueReview> starterSample(@Param("userId") UUID userId, @Param("limit") int limit);
}
