package uz.learnx.learnxapi.mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ActivityMapper {

    void record(@Param("userId") UUID userId, @Param("date") LocalDate date, @Param("points") int points);

    List<LocalDate> activeDays(@Param("userId") UUID userId, @Param("since") LocalDate since);

    int totalDays(@Param("userId") UUID userId);
}
