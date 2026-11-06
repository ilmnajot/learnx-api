package uz.learnx.learnxapi.activity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ActivityMapper {

    @Insert("""
            INSERT INTO activity_days (user_id, activity_date, points) VALUES (#{userId}, #{date}, #{points})
            ON CONFLICT (user_id, activity_date)
            DO UPDATE SET events = activity_days.events + 1, points = activity_days.points + EXCLUDED.points
            """)
    void record(@Param("userId") UUID userId, @Param("date") LocalDate date, @Param("points") int points);

    @Select("""
            SELECT activity_date FROM activity_days
            WHERE user_id = #{userId} AND activity_date >= #{since}
            ORDER BY activity_date DESC
            """)
    List<LocalDate> activeDays(@Param("userId") UUID userId, @Param("since") LocalDate since);

    @Select("SELECT count(*) FROM activity_days WHERE user_id = #{userId}")
    int totalDays(UUID userId);
}
