package uz.learnx.learnxapi.activity;

import java.util.UUID;

/**
 * Published inside the practising feature's transaction. Streaks and squads react to it, so lessons,
 * reviews and games stay unaware of those features.
 */
public record PracticeRecorded(UUID userId, Source source, int points) {

    public enum Source {
        LESSON,
        ROLEPLAY,
        REVIEW,
        GAME
    }
}
