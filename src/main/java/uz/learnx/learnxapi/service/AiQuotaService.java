package uz.learnx.learnxapi.service;

import java.util.UUID;

/**
 * Guards AI spending with three checks: a confirmed email (so throwaway accounts can't use AI), a per-learner daily
 * limit, and a daily ceiling across everyone.
 */
public interface AiQuotaService {
    void consume(UUID userId);
}
