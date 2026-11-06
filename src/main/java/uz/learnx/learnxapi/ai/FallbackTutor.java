package uz.learnx.learnxapi.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Keeps practice going when the primary tutor fails: an outage degrades feedback, not the whole lesson. */
public class FallbackTutor implements AiTutor {

    private static final Logger log = LoggerFactory.getLogger(FallbackTutor.class);

    private final AiTutor primary;
    private final AiTutor fallback;

    public FallbackTutor(AiTutor primary, AiTutor fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public RoleplayFeedback roleplayTurn(RoleplayRequest request) {
        try {
            return primary.roleplayTurn(request);
        } catch (RuntimeException e) {
            // Logs the failure type only: learner text must stay out of logs.
            log.warn("Primary tutor failed for a roleplay turn: {}", e.getClass().getSimpleName());
            return fallback.roleplayTurn(request);
        }
    }

    @Override
    public MessageFeedback improveMessage(String text, String nativeLanguage) {
        try {
            return primary.improveMessage(text, nativeLanguage);
        } catch (RuntimeException e) {
            log.warn("Primary tutor failed for a message: {}", e.getClass().getSimpleName());
            return fallback.improveMessage(text, nativeLanguage);
        }
    }
}
