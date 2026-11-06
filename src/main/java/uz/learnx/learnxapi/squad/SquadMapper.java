package uz.learnx.learnxapi.squad;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SquadMapper {

    record Squad(UUID id, String name, String inviteCode, UUID ownerId) {}

    record Incident(UUID id, String title, Integer points) {}

    record MemberRow(UUID userId, String displayName, Integer weekPoints, Boolean practisedToday) {}

    @Insert("""
            INSERT INTO squads (id, name, invite_code, owner_id)
            VALUES (#{id}, #{name}, #{inviteCode}, #{ownerId})
            """)
    void insertSquad(@Param("id") UUID id, @Param("name") String name, @Param("inviteCode") String inviteCode,
            @Param("ownerId") UUID ownerId);

    @Insert("INSERT INTO squad_members (squad_id, user_id) VALUES (#{squadId}, #{userId})")
    void addMember(@Param("squadId") UUID squadId, @Param("userId") UUID userId);

    @Select("""
            SELECT s.id, s.name, s.invite_code, s.owner_id
            FROM squads s JOIN squad_members m ON m.squad_id = s.id
            WHERE m.user_id = #{userId}
            """)
    Optional<Squad> findByMember(UUID userId);

    /** The row lock serialises joins, so the member limit holds under concurrent requests. */
    @Select("SELECT id, name, invite_code, owner_id FROM squads WHERE invite_code = #{code} FOR UPDATE")
    Optional<Squad> findByInviteCodeForUpdate(String code);

    @Select("SELECT id, name, invite_code, owner_id FROM squads WHERE id = #{id} FOR UPDATE")
    Optional<Squad> lock(UUID id);

    @Select("SELECT count(*) FROM squad_members WHERE squad_id = #{squadId}")
    int memberCount(UUID squadId);

    @Delete("DELETE FROM squad_members WHERE squad_id = #{squadId} AND user_id = #{userId}")
    void removeMember(@Param("squadId") UUID squadId, @Param("userId") UUID userId);

    @Delete("DELETE FROM squads WHERE id = #{id}")
    void deleteSquad(UUID id);

    @Update("""
            UPDATE squads SET owner_id = (
                SELECT user_id FROM squad_members WHERE squad_id = #{id} ORDER BY joined_at LIMIT 1)
            WHERE id = #{id}
            """)
    void transferOwnership(UUID id);

    @Insert("""
            INSERT INTO squad_incidents (squad_id, week_start, title) VALUES (#{squadId}, #{weekStart}, #{title})
            ON CONFLICT (squad_id, week_start) DO NOTHING
            """)
    void ensureIncident(@Param("squadId") UUID squadId, @Param("weekStart") LocalDate weekStart,
            @Param("title") String title);

    @Select("SELECT id, title, points FROM squad_incidents WHERE squad_id = #{squadId} AND week_start = #{weekStart}")
    Incident findIncident(@Param("squadId") UUID squadId, @Param("weekStart") LocalDate weekStart);

    @Update("UPDATE squad_incidents SET points = points + #{points} WHERE id = #{incidentId}")
    void addIncidentPoints(@Param("incidentId") UUID incidentId, @Param("points") int points);

    @Insert("""
            INSERT INTO squad_contributions (incident_id, user_id, points) VALUES (#{incidentId}, #{userId}, #{points})
            ON CONFLICT (incident_id, user_id)
            DO UPDATE SET points = squad_contributions.points + EXCLUDED.points
            """)
    void addContribution(@Param("incidentId") UUID incidentId, @Param("userId") UUID userId,
            @Param("points") int points);

    @Select("""
            SELECT m.user_id, u.display_name, COALESCE(c.points, 0) AS week_points,
                   EXISTS (SELECT 1 FROM activity_days a
                           WHERE a.user_id = m.user_id AND a.activity_date = #{today}) AS practised_today
            FROM squad_members m
            JOIN users u ON u.id = m.user_id
            LEFT JOIN squad_contributions c ON c.user_id = m.user_id AND c.incident_id = #{incidentId}
            WHERE m.squad_id = #{squadId}
            ORDER BY m.joined_at
            """)
    List<MemberRow> members(@Param("squadId") UUID squadId, @Param("incidentId") UUID incidentId,
            @Param("today") LocalDate today);

    /** Days on which enough of the current members practised. */
    @Select("""
            SELECT a.activity_date
            FROM activity_days a JOIN squad_members m ON m.user_id = a.user_id
            WHERE m.squad_id = #{squadId} AND a.activity_date >= #{since}
            GROUP BY a.activity_date
            HAVING count(*) >= #{required}
            """)
    List<LocalDate> qualifyingDays(@Param("squadId") UUID squadId, @Param("since") LocalDate since,
            @Param("required") int required);
}
