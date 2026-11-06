package uz.learnx.learnxapi.community;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Settings the admin controls at runtime. Donation details live here rather than in code, so they stay out of git. */
@Service
public class SiteSettings {

    /**
     * @param statsMinLearners public statistics stay hidden until this many learners exist, because a homepage saying
     *     "3 learners" works against you. Set it to 0 to always show them.
     */
    public record Values(
            @NotNull Boolean donationEnabled,
            @Pattern(regexp = "^$|^[0-9]{16}$", message = "Card number must be 16 digits") String donationCard,
            @Size(max = 60) String donationHolder,
            @Size(max = 300) String donationMessage,
            @NotNull Boolean commentsEnabled,
            @NotNull @Min(0) @Max(1_000_000) Integer statsMinLearners) {}

    @Mapper
    public interface SettingsMapper {
        record Row(String key, String value) {}

        @Select("SELECT key, value FROM site_settings")
        List<Row> all();

        @Insert("""
                INSERT INTO site_settings (key, value) VALUES (#{key}, #{value})
                ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value, updated_at = now()
                """)
        void put(@Param("key") String key, @Param("value") String value);
    }

    private static final int DEFAULT_STATS_MIN_LEARNERS = 50;

    private final SettingsMapper settings;

    SiteSettings(SettingsMapper settings) {
        this.settings = settings;
    }

    public Values load() {
        Map<String, String> values = settings.all().stream()
                .collect(Collectors.toMap(SettingsMapper.Row::key, SettingsMapper.Row::value));
        return new Values(
                Boolean.parseBoolean(values.getOrDefault("donation_enabled", "false")),
                values.getOrDefault("donation_card", ""),
                values.getOrDefault("donation_holder", ""),
                values.getOrDefault("donation_message", ""),
                Boolean.parseBoolean(values.getOrDefault("comments_enabled", "true")),
                Integer.parseInt(values.getOrDefault("stats_min_learners", String.valueOf(DEFAULT_STATS_MIN_LEARNERS))));
    }

    @Transactional
    public Values save(Values v) {
        settings.put("donation_enabled", v.donationEnabled().toString());
        settings.put("donation_card", clean(v.donationCard()));
        settings.put("donation_holder", clean(v.donationHolder()));
        settings.put("donation_message", clean(v.donationMessage()));
        settings.put("comments_enabled", v.commentsEnabled().toString());
        settings.put("stats_min_learners", v.statsMinLearners().toString());
        return load();
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
