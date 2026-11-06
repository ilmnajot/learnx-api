package uz.learnx.learnxapi.community;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;
import uz.learnx.learnxapi.common.config.LearnxProperties;
import uz.learnx.learnxapi.community.CommentMapper.PublicComment;

/**
 * Everything the public homepage shows. Cached briefly, so a traffic spike from a social media post doesn't turn into
 * a burst of database queries. All statistics are real counts, and individuals can't be identified from them.
 */
@Service
public class PublicSite {

    public record LevelCount(String level, Integer learners) {}

    public record Totals(Integer learners, Integer activeThisWeek, Integer lessonsCompleted, Integer practiceDays,
            Integer squads) {}

    public record Stats(Totals totals, List<LevelCount> levels) {}

    public record Donation(String card, String holder, String message) {}

    public record View(Stats stats, List<PublicComment> comments, Boolean commentsEnabled, Donation donation) {}

    @Mapper
    public interface PublicStatsMapper {
        @Select("""
                SELECT
                  (SELECT count(*) FROM users WHERE banned_at IS NULL AND email_verified_at IS NOT NULL)::int AS learners,
                  (SELECT count(DISTINCT a.user_id) FROM activity_days a JOIN users u ON u.id = a.user_id
                     WHERE a.activity_date >= #{weekAgo} AND u.banned_at IS NULL AND u.email_verified_at IS NOT NULL)::int
                     AS active_this_week,
                  (SELECT count(*) FROM lesson_progress)::int AS lessons_completed,
                  (SELECT count(*) FROM activity_days)::int AS practice_days,
                  (SELECT count(*) FROM squads)::int AS squads
                """)
        Totals totals(@Param("weekAgo") LocalDate weekAgo);

        @Select("""
                SELECT level, count(*)::int AS learners FROM users
                WHERE level IS NOT NULL AND banned_at IS NULL AND email_verified_at IS NOT NULL
                GROUP BY level ORDER BY level
                """)
        List<LevelCount> levels();
    }

    private static final int COMMENTS_SHOWN = 9;

    private record Cached(View view, Instant at) {}

    private final PublicStatsMapper stats;
    private final CommentMapper comments;
    private final SiteSettings settings;
    private final Duration cacheTtl;
    private final Clock clock;
    private volatile Cached cached;

    PublicSite(PublicStatsMapper stats, CommentMapper comments, SiteSettings settings, LearnxProperties properties,
            Clock clock) {
        this.stats = stats;
        this.comments = comments;
        this.settings = settings;
        this.cacheTtl = properties.publicSite().cacheTtl();
        this.clock = clock;
    }

    public View view() {
        Instant now = Instant.now(clock);
        Cached current = cached;
        if (current != null && current.at().plus(cacheTtl).isAfter(now)) {
            return current.view();
        }
        View fresh = build();
        cached = new Cached(fresh, now);
        return fresh;
    }

    private View build() {
        SiteSettings.Values values = settings.load();
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        Totals totals = stats.totals(today.minusDays(6));
        Stats publicStats = totals.learners() >= values.statsMinLearners() ? new Stats(totals, stats.levels()) : null;
        Donation donation = values.donationEnabled() && !values.donationCard().isBlank()
                ? new Donation(values.donationCard(), values.donationHolder(), values.donationMessage())
                : null;
        return new View(publicStats, comments.approved(COMMENTS_SHOWN), values.commentsEnabled(), donation);
    }
}
