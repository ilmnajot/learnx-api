package uz.learnx.learnxapi.service;

import java.util.UUID;
import uz.learnx.learnxapi.model.ApiResponse;

/** Read models that combine several features for the home and progress screens. */
public interface TodayService {
    ApiResponse today(UUID userId);

    ApiResponse progress(UUID userId);
}
