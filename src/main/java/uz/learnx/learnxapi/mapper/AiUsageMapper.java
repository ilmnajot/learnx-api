package uz.learnx.learnxapi.mapper;

import java.time.LocalDate;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Each limit is one atomic SQL statement, so it holds across server instances. */
@Mapper
public interface AiUsageMapper {

    /** @return 0 when the learner's daily limit is already reached */
    int consume(@Param("userId") UUID userId, @Param("date") LocalDate date, @Param("limit") int limit);

    /** @return 0 when the daily ceiling across everyone is already reached */
    int consumeGlobal(@Param("date") LocalDate date, @Param("limit") int limit);
}
