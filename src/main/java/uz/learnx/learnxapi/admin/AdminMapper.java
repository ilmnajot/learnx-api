package uz.learnx.learnxapi.admin;

import java.time.Instant;
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
public interface AdminMapper {

    record UserRow(UUID id, String email, String displayName, String jobRole, String level, String role,
            Instant emailVerifiedAt, Instant bannedAt, Instant createdAt, Integer lessonsCompleted,
            java.time.LocalDate lastActive) {}

    record LevelCount(String level, Integer count) {}

    record DayCount(java.time.LocalDate day, Integer count) {}

    record Totals(Integer users, Integer verified, Integer banned, Integer newThisWeek, Integer activeToday,
            Integer activeThisWeek, Integer lessonsCompleted, Integer videosCompleted, Integer squads,
            Integer pendingComments, Integer aiRequestsToday) {}

    record VideoRow(UUID id, String title, String channel, String category, String youtubeId, Instant hiddenAt,
            Integer completions) {}

    record AuditRow(UUID id, String adminEmail, String action, String target, Instant createdAt) {}

    @Select("""
            <script>
            SELECT u.id, u.email, u.display_name, u.job_role, u.level, u.role, u.email_verified_at, u.banned_at,
                   u.created_at,
                   (SELECT count(*) FROM lesson_progress p WHERE p.user_id = u.id)::int AS lessons_completed,
                   (SELECT max(a.activity_date) FROM activity_days a WHERE a.user_id = u.id) AS last_active
            FROM users u
            <where>
                <if test="search != null">
                    u.email ILIKE #{search} ESCAPE '\\' OR u.display_name ILIKE #{search} ESCAPE '\\'
                </if>
            </where>
            ORDER BY u.created_at DESC, u.id
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<UserRow> users(@Param("search") String search, @Param("limit") int limit, @Param("offset") int offset);

    @Select("""
            <script>
            SELECT count(*) FROM users u
            <where>
                <if test="search != null">
                    u.email ILIKE #{search} ESCAPE '\\' OR u.display_name ILIKE #{search} ESCAPE '\\'
                </if>
            </where>
            </script>
            """)
    int countUsers(@Param("search") String search);

    @Select("""
            SELECT
              (SELECT count(*) FROM users)::int AS users,
              (SELECT count(*) FROM users WHERE email_verified_at IS NOT NULL)::int AS verified,
              (SELECT count(*) FROM users WHERE banned_at IS NOT NULL)::int AS banned,
              (SELECT count(*) FROM users WHERE created_at >= #{weekAgo})::int AS new_this_week,
              (SELECT count(DISTINCT user_id) FROM activity_days WHERE activity_date = #{today})::int AS active_today,
              (SELECT count(DISTINCT user_id) FROM activity_days WHERE activity_date >= #{weekAgoDate})::int
                  AS active_this_week,
              (SELECT count(*) FROM lesson_progress)::int AS lessons_completed,
              (SELECT count(*) FROM video_progress)::int AS videos_completed,
              (SELECT count(*) FROM squads)::int AS squads,
              (SELECT count(*) FROM comments WHERE status = 'PENDING')::int AS pending_comments,
              COALESCE((SELECT requests FROM ai_global_usage WHERE usage_date = #{today}), 0) AS ai_requests_today
            """)
    Totals totals(@Param("today") java.time.LocalDate today, @Param("weekAgoDate") java.time.LocalDate weekAgoDate,
            @Param("weekAgo") Instant weekAgo);

    @Select("""
            SELECT level, count(*)::int AS count FROM users
            WHERE level IS NOT NULL AND banned_at IS NULL
            GROUP BY level ORDER BY level
            """)
    List<LevelCount> levels();

    @Select("""
            SELECT (created_at AT TIME ZONE 'UTC')::date AS day, count(*)::int AS count FROM users
            WHERE created_at >= #{since}
            GROUP BY 1 ORDER BY 1
            """)
    List<DayCount> signupsByDay(Instant since);

    @Update("UPDATE users SET banned_at = #{at}, updated_at = now() WHERE id = #{id}")
    int setBanned(@Param("id") UUID id, @Param("at") Instant at);

    @Update("UPDATE users SET role = #{role}, updated_at = now() WHERE id = #{id}")
    int setRole(@Param("id") UUID id, @Param("role") String role);

    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteUser(UUID id);

    @Select("""
            SELECT v.id, v.title, v.channel, v.category, v.youtube_id, v.hidden_at,
                   (SELECT count(*) FROM video_progress p WHERE p.video_id = v.id)::int AS completions
            FROM videos v ORDER BY v.position
            """)
    List<VideoRow> videos();

    @Update("UPDATE videos SET hidden_at = #{at} WHERE id = #{id}")
    int setVideoHidden(@Param("id") UUID id, @Param("at") Instant at);

    @Insert("""
            INSERT INTO admin_audit (admin_id, admin_email, action, target)
            VALUES (#{adminId}, #{adminEmail}, #{action}, #{target})
            """)
    void audit(@Param("adminId") UUID adminId, @Param("adminEmail") String adminEmail, @Param("action") String action,
            @Param("target") String target);

    @Select("""
            SELECT id, admin_email, action, target, created_at FROM admin_audit
            ORDER BY created_at DESC LIMIT #{limit}
            """)
    List<AuditRow> auditLog(int limit);

    @Select("SELECT count(*) FROM users WHERE role = 'ADMIN' AND banned_at IS NULL")
    int countAdmins();

    @Select("SELECT email FROM users WHERE id = #{id}")
    Optional<String> emailOf(UUID id);
}
