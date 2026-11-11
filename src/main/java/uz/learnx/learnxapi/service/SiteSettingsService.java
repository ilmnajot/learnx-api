package uz.learnx.learnxapi.service;

import uz.learnx.learnxapi.dto.SiteDto;

/** Settings the admin controls at runtime. Donation details live here rather than in code, so they stay out of git. */
public interface SiteSettingsService {
    SiteDto.Settings load();

    SiteDto.Settings save(SiteDto.Settings settings);
}
