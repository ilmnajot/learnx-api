package uz.learnx.learnxapi.game;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.learnx.learnxapi.activity.PracticeRecorded;
import uz.learnx.learnxapi.review.ReviewMapper.DueReview;
import uz.learnx.learnxapi.review.ReviewService;
import uz.learnx.learnxapi.user.UserClock;

@Service
public class GameService {

    /** Games are played in the browser, so their points are capped: results can't be trusted to earn much. */
    static final int MAX_POINTS_PER_GAME = 5;

    static final int SCORING_GAMES_PER_DAY = 3;

    public enum Game {
        PHRASE_MATCH,
        WORD_ORDER,
        SHADOWING
    }

    public record Result(int points, int gamesScoredToday) {}

    @Mapper
    public interface GameMapper {
        @Insert("""
                INSERT INTO game_results (user_id, game, correct, total, points, played_on)
                VALUES (#{userId}, #{game}, #{correct}, #{total}, #{points}, #{day})
                """)
        void insert(@Param("userId") UUID userId, @Param("game") String game, @Param("correct") int correct,
                @Param("total") int total, @Param("points") int points, @Param("day") java.time.LocalDate day);

        @Select("SELECT count(*) FROM game_results WHERE user_id = #{userId} AND played_on = #{day} AND points > 0")
        int scoringGames(@Param("userId") UUID userId, @Param("day") java.time.LocalDate day);
    }

    private final GameMapper games;
    private final ReviewService reviews;
    private final UserClock userClock;
    private final ApplicationEventPublisher events;

    GameService(GameMapper games, ReviewService reviews, UserClock userClock, ApplicationEventPublisher events) {
        this.games = games;
        this.reviews = reviews;
        this.userClock = userClock;
        this.events = events;
    }

    public List<DueReview> pool(UUID userId, int size) {
        return reviews.gamePool(userId, size);
    }

    @Transactional
    public Result record(UUID userId, Game game, int correct, int total) {
        var today = userClock.today(userId);
        int scored = games.scoringGames(userId, today);
        int points = scored < SCORING_GAMES_PER_DAY ? Math.min(correct, MAX_POINTS_PER_GAME) : 0;
        games.insert(userId, game.name(), correct, total, points, today);
        events.publishEvent(new PracticeRecorded(userId, PracticeRecorded.Source.GAME, points));
        return new Result(points, points > 0 ? scored + 1 : scored);
    }
}
