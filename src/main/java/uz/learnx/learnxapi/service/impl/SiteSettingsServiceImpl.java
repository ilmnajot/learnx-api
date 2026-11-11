package uz.learnx.learnxapi.service.impl;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.learnx.learnxapi.dto.SiteDto;
import uz.learnx.learnxapi.mapper.SiteMapper;
import uz.learnx.learnxapi.service.SiteSettingsService;

@Service
@RequiredArgsConstructor
public class SiteSettingsServiceImpl implements SiteSettingsService {

    private static final int DEFAULT_STATS_MIN_LEARNERS = 50;

    private final SiteMapper siteMapper;

    @Override
    public SiteDto.Settings load() {
        Map<String, String> values = this.siteMapper.allSettings().stream()
                .collect(Collectors.toMap(SiteDto.SettingRow::key, SiteDto.SettingRow::value));
        return new SiteDto.Settings(
                Boolean.parseBoolean(values.getOrDefault("donation_enabled", "false")),
                values.getOrDefault("donation_card", ""),
                values.getOrDefault("donation_holder", ""),
                values.getOrDefault("donation_message", ""),
                Boolean.parseBoolean(values.getOrDefault("comments_enabled", "true")),
                Integer.parseInt(values.getOrDefault("stats_min_learners", String.valueOf(DEFAULT_STATS_MIN_LEARNERS))));
    }

    @Override
    @Transactional
    public SiteDto.Settings save(SiteDto.Settings s) {
        this.siteMapper.putSetting("donation_enabled", s.donationEnabled().toString());
        this.siteMapper.putSetting("donation_card", clean(s.donationCard()));
        this.siteMapper.putSetting("donation_holder", clean(s.donationHolder()));
        this.siteMapper.putSetting("donation_message", clean(s.donationMessage()));
        this.siteMapper.putSetting("comments_enabled", s.commentsEnabled().toString());
        this.siteMapper.putSetting("stats_min_learners", s.statsMinLearners().toString());
        return load();
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
