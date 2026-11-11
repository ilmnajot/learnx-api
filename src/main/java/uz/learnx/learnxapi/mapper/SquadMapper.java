package uz.learnx.learnxapi.mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import uz.learnx.learnxapi.dto.SquadDto;

@Mapper
public interface SquadMapper {

    void insertSquad(@Param("id") UUID id, @Param("name") String name, @Param("inviteCode") String inviteCode,
            @Param("ownerId") UUID ownerId);

    void addMember(@Param("squadId") UUID squadId, @Param("userId") UUID userId);

    Optional<SquadDto.Squad> findByMember(@Param("userId") UUID userId);

    /** The row lock serialises joins, so the member limit holds under concurrent requests. */
    Optional<SquadDto.Squad> findByInviteCodeForUpdate(@Param("code") String code);

    Optional<SquadDto.Squad> lock(@Param("id") UUID id);

    int memberCount(@Param("squadId") UUID squadId);

    void removeMember(@Param("squadId") UUID squadId, @Param("userId") UUID userId);

    void deleteSquad(@Param("id") UUID id);

    void transferOwnership(@Param("id") UUID id);

    void ensureIncident(@Param("squadId") UUID squadId, @Param("weekStart") LocalDate weekStart,
            @Param("title") String title);

    SquadDto.Incident findIncident(@Param("squadId") UUID squadId, @Param("weekStart") LocalDate weekStart);

    void addIncidentPoints(@Param("incidentId") UUID incidentId, @Param("points") int points);

    void addContribution(@Param("incidentId") UUID incidentId, @Param("userId") UUID userId,
            @Param("points") int points);

    List<SquadDto.MemberRow> members(@Param("squadId") UUID squadId, @Param("incidentId") UUID incidentId,
            @Param("today") LocalDate today);

    /** Days on which enough of the current members practised. */
    List<LocalDate> qualifyingDays(@Param("squadId") UUID squadId, @Param("since") LocalDate since,
            @Param("required") int required);
}
