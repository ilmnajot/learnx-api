package uz.learnx.learnxapi.mapper;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import uz.learnx.learnxapi.dto.User;

@Mapper
public interface UserMapper {

    Optional<User> findById(@Param("id") UUID id);

    Optional<User> findByEmail(@Param("email") String email);

    Optional<String> findTimezone(@Param("id") UUID id);

    void insert(@Param("id") UUID id, @Param("email") String email, @Param("passwordHash") String passwordHash,
            @Param("displayName") String displayName);

    void updateProfile(@Param("id") UUID id, @Param("displayName") String displayName,
            @Param("jobRole") String jobRole, @Param("goal") String goal, @Param("timezone") String timezone,
            @Param("reminderTime") LocalTime reminderTime);

    void updateLevel(@Param("id") UUID id, @Param("level") String level);

    void markOnboarded(@Param("id") UUID id, @Param("at") Instant at);

    void recordFailedLogin(@Param("id") UUID id, @Param("maxAttempts") int maxAttempts,
            @Param("lockUntil") Instant lockUntil);

    void clearFailedLogins(@Param("id") UUID id);

    void markVerified(@Param("id") UUID id, @Param("at") Instant at);

    void updatePassword(@Param("id") UUID id, @Param("passwordHash") String passwordHash);

    /** Only confirmed emails are promoted, so nobody can register an admin's address first and take over. */
    int promoteToAdmin(@Param("emails") List<String> emails);
}
