package uz.learnx.learnxapi.service;

/**
 * Makes the addresses in LEARNX_ADMIN_EMAILS admins, but only once the email is confirmed. Otherwise anyone could
 * register an admin's address before they do and take over the panel.
 */
public interface AdminPromotionService {
    /** Called at startup and after any email is confirmed. */
    void promoteConfigured();

    boolean isConfigured(String email);
}
