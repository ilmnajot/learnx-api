package uz.learnx.learnxapi.service.impl;

import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.learnx.learnxapi.dto.GameDto;
import uz.learnx.learnxapi.event.PracticeRecorded;
import uz.learnx.learnxapi.mapper.GameMapper;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.service.GameService;
import uz.learnx.learnxapi.service.ReviewService;
import uz.learnx.learnxapi.service.UserService;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    /** Games are played in the browser, so their points are capped: results can't be trusted to earn much. */
    static final int MAX_POINTS_PER_GAME = 5;

    static final int SCORING_GAMES_PER_DAY = 3;

    private final GameMapper gameMapper;
    private final ReviewService reviewService;
    private final UserService userService;
    private final ApplicationEventPublisher events;

    @Override
    public ApiResponse pool(UUID userId, int size) {
        return ApiResponse.ok(this.reviewService.gamePool(userId, size));
    }

    @Override
    @Transactional
    public ApiResponse record(UUID userId, GameDto.ResultRequest request) {
        LocalDate today = this.userService.today(userId);
        int scored = this.gameMapper.scoringGames(userId, today);
        int points = scored < SCORING_GAMES_PER_DAY ? Math.min(request.correct(), MAX_POINTS_PER_GAME) : 0;
        this.gameMapper.insert(userId, request.game().name(), request.correct(), request.total(), points, today);
        this.events.publishEvent(new PracticeRecorded(userId, PracticeRecorded.Source.GAME, points));
        return ApiResponse.ok(new GameDto.Result(points, points > 0 ? scored + 1 : scored));
    }
}
