package uz.learnx.learnxapi.squad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SquadRulesTest {

    @Test
    void oneBusyPersonDoesNotBreakTheStreak() {
        assertThat(SquadRules.requiredToday(1)).isEqualTo(1);
        assertThat(SquadRules.requiredToday(3)).isEqualTo(2);
        assertThat(SquadRules.requiredToday(4)).isEqualTo(3);
        assertThat(SquadRules.requiredToday(5)).isEqualTo(3);
        assertThat(SquadRules.requiredToday(8)).isEqualTo(5);
    }

    @Test
    void progressIsCappedAtOneHundredPercent() {
        assertThat(SquadRules.progressPercent(80, 160)).isEqualTo(50);
        assertThat(SquadRules.progressPercent(500, 160)).isEqualTo(100);
    }

    @Test
    void consecutiveWeeksGetDifferentIncidents() {
        LocalDate monday = LocalDate.of(2026, 11, 2);
        assertThat(SquadRules.incidentTitle(monday)).isNotEqualTo(SquadRules.incidentTitle(monday.plusWeeks(1)));
        assertThat(SquadRules.incidentTitle(monday)).isEqualTo(SquadRules.incidentTitle(monday));
    }
}
