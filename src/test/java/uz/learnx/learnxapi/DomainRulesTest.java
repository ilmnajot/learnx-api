package uz.learnx.learnxapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import uz.learnx.learnxapi.activity.StreakCalculator;
import uz.learnx.learnxapi.ai.AiTutor;
import uz.learnx.learnxapi.ai.FallbackTutor;
import uz.learnx.learnxapi.ai.RuleBasedTutor;
import uz.learnx.learnxapi.review.ReviewScheduler;

class DomainRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    @Test
    void streakCountsConsecutiveDaysEndingToday() {
        assertThat(StreakCalculator.current(List.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2)), TODAY)).isEqualTo(3);
    }

    @Test
    void streakEndingYesterdayIsStillAlive() {
        assertThat(StreakCalculator.current(List.of(TODAY.minusDays(1), TODAY.minusDays(2)), TODAY)).isEqualTo(2);
    }

    @Test
    void missedDayBreaksTheStreak() {
        assertThat(StreakCalculator.current(List.of(TODAY, TODAY.minusDays(2)), TODAY)).isEqualTo(1);
        assertThat(StreakCalculator.current(List.of(TODAY.minusDays(2)), TODAY)).isZero();
        assertThat(StreakCalculator.current(List.of(), TODAY)).isZero();
    }

    @Test
    void rememberedPhrasesComeBackAfterGrowingGaps() {
        ReviewScheduler.State s = new ReviewScheduler.State(new BigDecimal("2.50"), 0, 0, 0);
        s = ReviewScheduler.next(s, true);
        assertThat(s.intervalDays()).isEqualTo(1);
        s = ReviewScheduler.next(s, true);
        assertThat(s.intervalDays()).isEqualTo(3);
        s = ReviewScheduler.next(s, true);
        assertThat(s.intervalDays()).isEqualTo(7);
        s = ReviewScheduler.next(s, true);
        assertThat(s.intervalDays()).isGreaterThan(7);
    }

    @Test
    void missedPhraseComesBackTomorrowWithLowerEase() {
        ReviewScheduler.State s = new ReviewScheduler.State(new BigDecimal("2.50"), 30, 6, 0);
        ReviewScheduler.State next = ReviewScheduler.next(s, false);
        assertThat(next.intervalDays()).isEqualTo(1);
        assertThat(next.repetitions()).isZero();
        assertThat(next.lapses()).isEqualTo(1);
        assertThat(next.ease()).isLessThan(s.ease());
    }

    @Test
    void easeAndIntervalStayWithinBounds() {
        ReviewScheduler.State low = ReviewScheduler.next(new ReviewScheduler.State(new BigDecimal("1.30"), 1, 0, 0), false);
        assertThat(low.ease()).isEqualByComparingTo("1.30");
        ReviewScheduler.State high = ReviewScheduler.next(new ReviewScheduler.State(new BigDecimal("3.00"), 170, 9, 0), true);
        assertThat(high.ease()).isEqualByComparingTo("3.00");
        assertThat(high.intervalDays()).isEqualTo(180);
    }

    @Test
    void ruleBasedTutorFixesCommonMistakes() {
        AiTutor.MessageFeedback result = new RuleBasedTutor()
                .improveMessage("no, it is not finish, i have problem with database", "uz");
        assertThat(result.improved()).isEqualTo("No, it is not finished, I have a problem with database.");
        assertThat(result.tips()).hasSize(2);
        assertThat(result.provider()).isEqualTo(RuleBasedTutor.PROVIDER);
    }

    @Test
    void ruleBasedTutorOnlyAddsTheObjectWhenTheSentenceHasNone() {
        RuleBasedTutor tutor = new RuleBasedTutor();
        assertThat(tutor.improveMessage("i will send you tomorrow", "uz").improved())
                .isEqualTo("I will send it to you tomorrow.");
        assertThat(tutor.improveMessage("i will send you tomorrow the report", "uz").improved())
                .isEqualTo("I will send you tomorrow the report.");
    }

    @Test
    void ruleBasedTutorRestoresBaseVerbAfterDidNot() {
        assertThat(new RuleBasedTutor().improveMessage("I didn't merged it", "uz").improved())
                .isEqualTo("I didn't merge it.");
        assertThat(new RuleBasedTutor().improveMessage("I didn't fixed it", "uz").improved())
                .isEqualTo("I didn't fix it.");
    }

    @Test
    void ruleBasedTutorClosesTheConversationOnTheFinalTurn() {
        AiTutor.RoleplayRequest request = new AiTutor.RoleplayRequest("s", "p", "uz", List.of(), "line", "hello", true);
        assertThat(new RuleBasedTutor().roleplayTurn(request).reply()).doesNotContain("?");
    }

    @Test
    void fallbackTutorTakesOverWhenPrimaryFails() {
        AiTutor failing = new AiTutor() {
            @Override
            public RoleplayFeedback roleplayTurn(RoleplayRequest request) {
                throw new IllegalStateException("outage");
            }

            @Override
            public MessageFeedback improveMessage(String text, String nativeLanguage) {
                throw new IllegalStateException("outage");
            }
        };
        AiTutor tutor = new FallbackTutor(failing, new RuleBasedTutor());
        assertThat(tutor.improveMessage("hello", "uz").provider()).isEqualTo(RuleBasedTutor.PROVIDER);
        assertThrows(IllegalStateException.class, () -> failing.improveMessage("hello", "uz"));
    }
}
