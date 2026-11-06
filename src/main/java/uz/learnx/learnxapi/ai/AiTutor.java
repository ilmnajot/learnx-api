package uz.learnx.learnxapi.ai;

import java.util.List;

/** The application's view of a language coach. Implementations can be swapped without touching features. */
public interface AiTutor {

    record RoleplayExchange(String coachLine, String learnerText) {}

    record RoleplayRequest(String scenario, String persona, String nativeLanguage, List<RoleplayExchange> history,
            String coachLine, String learnerText, boolean finalTurn) {}

    record RoleplayFeedback(String improved, List<String> tips, String reply, String provider) {}

    record MessageFeedback(String improved, List<String> tips, String provider) {}

    RoleplayFeedback roleplayTurn(RoleplayRequest request);

    MessageFeedback improveMessage(String text, String nativeLanguage);
}
