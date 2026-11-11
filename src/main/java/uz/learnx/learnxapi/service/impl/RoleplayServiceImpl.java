package uz.learnx.learnxapi.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import uz.learnx.learnxapi.dto.LearningDto;
import uz.learnx.learnxapi.dto.RoleplayDto;
import uz.learnx.learnxapi.event.PracticeRecorded;
import uz.learnx.learnxapi.exception.ApiException;
import uz.learnx.learnxapi.integration.AiTutor;
import uz.learnx.learnxapi.mapper.RoleplayMapper;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.service.AiQuotaService;
import uz.learnx.learnxapi.service.LearningService;
import uz.learnx.learnxapi.service.RoleplayService;
import uz.learnx.learnxapi.service.UserService;

/**
 * Runs roleplay conversations. The AI call happens outside any transaction, so a slow provider
 * never holds a database connection. A unique constraint rejects double-submitted turns.
 */
@Service
@RequiredArgsConstructor
public class RoleplayServiceImpl implements RoleplayService {

    static final int MAX_TURNS = 3;
    static final int TURN_POINTS = 3;

    private final RoleplayMapper roleplayMapper;
    private final LearningService learningService;
    private final UserService userService;
    private final AiTutor aiTutor;
    private final AiQuotaService aiQuotaService;
    private final TransactionTemplate tx;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    public ApiResponse start(UUID userId, UUID lessonId) {
        LearningDto.Lesson lesson = this.learningService.requireLesson(lessonId);
        if (lesson.aiPersona().isBlank()) {
            throw ApiException.conflict("roleplay_unavailable", "This lesson has no conversation practice");
        }
        UUID sessionId = UUID.randomUUID();
        this.roleplayMapper.insertSession(sessionId, userId, lessonId, MAX_TURNS);
        return ApiResponse.created(
                new RoleplayDto.Started(sessionId, lesson.openingLine(), lesson.scenario(), MAX_TURNS));
    }

    @Override
    public ApiResponse takeTurn(UUID userId, UUID sessionId, RoleplayDto.TurnRequest request) {
        String learnerText = request.text().strip();
        RoleplayDto.Session session = this.roleplayMapper.findSession(sessionId, userId)
                .orElseThrow(() -> ApiException.notFound("Roleplay session"));
        if (session.completedAt() != null) {
            throw ApiException.conflict("roleplay_finished", "This roleplay has already finished");
        }
        LearningDto.Lesson lesson = this.learningService.requireLesson(session.lessonId());
        List<RoleplayDto.Turn> previous = this.roleplayMapper.turns(sessionId);
        String coachLine = previous.isEmpty() ? lesson.openingLine() : previous.getLast().replyLine();
        int turnNumber = previous.size() + 1;
        boolean finalTurn = turnNumber >= session.maxTurns();

        this.aiQuotaService.consume(userId);
        AiTutor.RoleplayFeedback feedback = this.aiTutor.roleplayTurn(new AiTutor.RoleplayRequest(
                lesson.scenario(),
                lesson.aiPersona(),
                this.userService.getUser(userId).nativeLanguage(),
                previous.stream().map(t -> new AiTutor.RoleplayExchange(t.promptLine(), t.userText())).toList(),
                coachLine,
                learnerText,
                finalTurn));

        RoleplayDto.Turn turn = new RoleplayDto.Turn(turnNumber, coachLine, learnerText, feedback.improved(),
                feedback.tips(), feedback.reply(), feedback.provider());
        Instant now = Instant.now(this.clock);
        try {
            this.tx.executeWithoutResult(status -> {
                this.roleplayMapper.insertTurn(sessionId, turn);
                this.roleplayMapper.advance(sessionId, turnNumber, now);
                this.events.publishEvent(new PracticeRecorded(userId, PracticeRecorded.Source.ROLEPLAY, TURN_POINTS));
            });
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("turn_already_submitted", "This turn was already submitted");
        }
        return ApiResponse.ok(new RoleplayDto.TurnResult(turnNumber, feedback.improved(), feedback.tips(),
                feedback.reply(), finalTurn, feedback.provider()));
    }
}
