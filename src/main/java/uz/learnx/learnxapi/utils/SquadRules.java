package uz.learnx.learnxapi.utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** The pure rules of squad play. */
public final class SquadRules {

    public static final int MAX_MEMBERS = 8;
    private static final int POINTS_PER_MEMBER_PER_WEEK = 40;

    private static final List<String> INCIDENTS = List.of(
            "Checkout service is down",
            "Login is timing out",
            "Database connections are exhausted",
            "Payments are failing after a deploy",
            "The message queue is backing up",
            "Search returns no results",
            "An expired certificate broke the API",
            "Disk is full on the main server");

    private SquadRules() {}

    /** Most of the squad must practise (60%, rounded up), so one busy person never breaks the streak. */
    public static int requiredToday(int members) {
        return Math.max(1, (int) Math.ceil(members * 0.6));
    }

    public static int incidentTarget(int members) {
        return Math.max(1, members) * POINTS_PER_MEMBER_PER_WEEK;
    }

    public static int progressPercent(int points, int target) {
        return Math.min(100, points * 100 / target);
    }

    /** The same week always gets the same incident, and consecutive weeks get different ones. */
    public static String incidentTitle(LocalDate weekStart) {
        long weeks = ChronoUnit.WEEKS.between(LocalDate.EPOCH, weekStart);
        return INCIDENTS.get((int) Math.floorMod(weeks, (long) INCIDENTS.size()));
    }
}
