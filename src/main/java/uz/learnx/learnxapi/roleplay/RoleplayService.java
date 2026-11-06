package uz.learnx.learnxapi.roleplay;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import uz.learnx.learnxapi.activity.PracticeRecorded;
import uz.learnx.learnxapi.ai.AiQuota;
import uz.learnx.learnxapi.ai.AiTutor;
import uz.learnx.learnxapi.common.error.ApiException;
import uz.learnx.learnxapi.learning.LearningMapper.Lesson;
import uz.learnx.learnxapi.learning.LearningService;
import uz.learnx.learnxapi.roleplay.RoleplayMapper.Session;
import uz.learnx.learnxapi.roleplay.RoleplayMapper.Turn;
import uz.learnx.learnxapi.user.UserService;

/**
 * Runs roleplay conversations. The AI call happens outside any transaction, so a slow provider
 * never holds a database connection. A unique constraint rejects double-submitted turns.
 */
@Service
public class RoleplayService {

    static final int MAX_TURNS = 3;
    static final int TURN_POINTS = 3;

    public record Started(UUID sessionId, String openingLine, String scenario, int maxTurns) {}

    public record TurnResult(int turnNumber, String improved, List<String> tips, String reply, boolean finished,
            String provider) {}

    private final RoleplayMapper roleplay;
    private final LearningService learning;
    private final UserService users;
    private final AiTutor tutor;
    private final AiQuota quota;
    private final TransactionTemplate tx;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    RoleplayService(RoleplayMapper roleplay, LearningService learning, UserService users, AiTutor tutor, AiQuota quota,
            TransactionTemplate tx, ApplicationEventPublisher events, Clock clock) {
        this.roleplay = roleplay;
        this.learning = learning;
        this.users = users;
        this.tutor = tutor;
        this.quota = quota;
        this.tx = tx;
        this.events = events;
        this.clock = clock;
    }

    public Started start(UUID userId, UUID lessonId) {
        Lesson lesson = learning.requireLesson(lessonId);
        UUID sessionId = UUID.randomUUID();
        roleplay.insertSession(sessionId, userId, lessonId, MAX_TURNS);
        return new Started(sessionId, lesson.openingLine(), lesson.scenario(), MAX_TURNS);
    }

    public TurnResult takeTurn(UUID userId, UUID sessionId, String learnerText) {
        Session session = roleplay.findSession(sessionId, userId)
                .orElseThrow(() -> ApiException.notFound("Roleplay session"));
        if (session.completedAt() != null) {
            throw ApiException.conflict("roleplay_finished", "This roleplay has already finished");
        }
        Lesson lesson = learning.requireLesson(session.lessonId());
        List<Turn> previous = roleplay.turns(sessionId);
        String coachLine = previous.isEmpty() ? lesson.openingLine() : previous.getLast().replyLine();
        int turnNumber = previous.size() + 1;
        boolean finalTurn = turnNumber >= session.maxTurns();

        quota.consume(userId);
        AiTutor.RoleplayFeedback feedback = tutor.roleplayTurn(new AiTutor.RoleplayRequest(
                lesson.scenario(),
                lesson.aiPersona(),
                users.get(userId).nativeLanguage(),
                previous.stream().map(t -> new AiTutor.RoleplayExchange(t.promptLine(), t.userText())).toList(),
                coachLine,
                learnerText,
                finalTurn));

        Turn turn = new Turn(turnNumber, coachLine, learnerText, feedback.improved(), feedback.tips(), feedback.reply(),
                feedback.provider());
        Instant now = Instant.now(clock);
        try {
            tx.executeWithoutResult(status -> {
                roleplay.insertTurn(sessionId, turn);
                roleplay.advance(sessionId, turnNumber, now);
                events.publishEvent(new PracticeRecorded(userId, PracticeRecorded.Source.ROLEPLAY, TURN_POINTS));
            });
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("turn_already_submitted", "This turn was already submitted");
        }
        return new TurnResult(turnNumber, feedback.improved(), feedback.tips(), feedback.reply(), finalTurn,
                feedback.provider());
    }
}
