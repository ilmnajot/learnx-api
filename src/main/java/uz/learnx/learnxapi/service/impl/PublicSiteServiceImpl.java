package uz.learnx.learnxapi.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import uz.learnx.learnxapi.config.LearnxProperties;
import uz.learnx.learnxapi.dto.SiteDto;
import uz.learnx.learnxapi.mapper.CommentMapper;
import uz.learnx.learnxapi.mapper.SiteMapper;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.service.PublicSiteService;
import uz.learnx.learnxapi.service.SiteSettingsService;

/**
 * Cached briefly, so a traffic spike from a social media post doesn't turn into a burst of database queries.
 * All statistics are real counts, and individuals can't be identified from them.
 */
@Service
public class PublicSiteServiceImpl implements PublicSiteService {

    private static final int COMMENTS_SHOWN = 9;

    private record Cached(SiteDto.View view, Instant at) {}

    private final SiteMapper siteMapper;
    private final CommentMapper commentMapper;
    private final SiteSettingsService siteSettingsService;
    private final Duration cacheTtl;
    private final Clock clock;
    private volatile Cached cached;

    public PublicSiteServiceImpl(SiteMapper siteMapper, CommentMapper commentMapper,
            SiteSettingsService siteSettingsService, LearnxProperties properties, Clock clock) {
        this.siteMapper = siteMapper;
        this.commentMapper = commentMapper;
        this.siteSettingsService = siteSettingsService;
        this.cacheTtl = properties.publicSite().cacheTtl();
        this.clock = clock;
    }

    @Override
    public ApiResponse site() {
        Instant now = Instant.now(this.clock);
        Cached current = this.cached;
        if (current != null && current.at().plus(this.cacheTtl).isAfter(now)) {
            return ApiResponse.ok(current.view());
        }
        SiteDto.View fresh = build();
        this.cached = new Cached(fresh, now);
        return ApiResponse.ok(fresh);
    }

    private SiteDto.View build() {
        SiteDto.Settings settings = this.siteSettingsService.load();
        LocalDate today = LocalDate.now(this.clock.withZone(ZoneOffset.UTC));
        SiteDto.Totals totals = this.siteMapper.publicTotals(today.minusDays(6));
        SiteDto.Stats stats = totals.learners() >= settings.statsMinLearners()
                ? new SiteDto.Stats(totals, this.siteMapper.publicLevels())
                : null;
        SiteDto.Donation donation = settings.donationEnabled() && !settings.donationCard().isBlank()
                ? new SiteDto.Donation(settings.donationCard(), settings.donationHolder(), settings.donationMessage())
                : null;
        return new SiteDto.View(stats, this.commentMapper.approved(COMMENTS_SHOWN), settings.commentsEnabled(),
                donation);
    }
}
