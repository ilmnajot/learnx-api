package uz.learnx.learnxapi.mapper;

import java.time.LocalDate;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GameMapper {

    void insert(@Param("userId") UUID userId, @Param("game") String game, @Param("correct") int correct,
            @Param("total") int total, @Param("points") int points, @Param("day") LocalDate day);

    int scoringGames(@Param("userId") UUID userId, @Param("day") LocalDate day);
}
