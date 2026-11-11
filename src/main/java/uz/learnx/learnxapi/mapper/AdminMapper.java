package uz.learnx.learnxapi.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import uz.learnx.learnxapi.dto.AdminDto;

@Mapper
public interface AdminMapper {

    List<AdminDto.UserRow> users(@Param("search") String search, @Param("limit") int limit,
            @Param("offset") int offset);

    int countUsers(@Param("search") String search);

    AdminDto.Totals totals(@Param("today") LocalDate today, @Param("weekAgoDate") LocalDate weekAgoDate,
            @Param("weekAgo") Instant weekAgo);

    List<AdminDto.LevelCount> levels();

    List<AdminDto.DayCount> signupsByDay(@Param("since") Instant since);

    int setBanned(@Param("id") UUID id, @Param("at") Instant at);

    int setRole(@Param("id") UUID id, @Param("role") String role);

    int deleteUser(@Param("id") UUID id);

    List<AdminDto.VideoRow> videos();

    int setVideoHidden(@Param("id") UUID id, @Param("at") Instant at);

    void audit(@Param("adminId") UUID adminId, @Param("adminEmail") String adminEmail, @Param("action") String action,
            @Param("target") String target);

    List<AdminDto.AuditRow> auditLog(@Param("limit") int limit);

    Optional<String> emailOf(@Param("id") UUID id);
}
