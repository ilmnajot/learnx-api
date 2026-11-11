package uz.learnx.learnxapi.mapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import uz.learnx.learnxapi.dto.RoleplayDto;

@Mapper
public interface RoleplayMapper {

    void insertSession(@Param("id") UUID id, @Param("userId") UUID userId, @Param("lessonId") UUID lessonId,
            @Param("maxTurns") int maxTurns);

    Optional<RoleplayDto.Session> findSession(@Param("id") UUID id, @Param("userId") UUID userId);

    List<RoleplayDto.Turn> turns(@Param("sessionId") UUID sessionId);

    void insertTurn(@Param("sessionId") UUID sessionId, @Param("turn") RoleplayDto.Turn turn);

    void advance(@Param("sessionId") UUID sessionId, @Param("turnNumber") int turnNumber, @Param("now") Instant now);
}
