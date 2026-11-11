package uz.learnx.learnxapi.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.learnx.learnxapi.dto.PlacementDto;
import uz.learnx.learnxapi.exception.ApiException;
import uz.learnx.learnxapi.mapper.PlacementMapper;
import uz.learnx.learnxapi.mapper.UserMapper;
import uz.learnx.learnxapi.model.ApiResponse;
import uz.learnx.learnxapi.service.PlacementService;

@Service
@RequiredArgsConstructor
public class PlacementServiceImpl implements PlacementService {

    private final PlacementMapper placementMapper;
    private final UserMapper userMapper;

    @Override
    public ApiResponse questions() {
        return ApiResponse.ok(this.placementMapper.questions().stream()
                .map(q -> new PlacementDto.QuestionView(q.id(), q.prompt(), q.options()))
                .toList());
    }

    /** Unknown question ids are ignored, and only the first answer to a question counts. */
    @Override
    @Transactional
    public ApiResponse submit(UUID userId, PlacementDto.SubmitRequest request) {
        Map<UUID, Integer> selected = request.answers().stream().collect(Collectors.toMap(
                PlacementDto.Answer::questionId, PlacementDto.Answer::selectedIndex, (first, second) -> first));
        List<PlacementDto.Question> questions = this.placementMapper.questions();
        if (questions.isEmpty()) {
            throw ApiException.notFound("Placement test");
        }
        int max = 0;
        int score = 0;
        for (PlacementDto.Question q : questions) {
            max += q.weight();
            if (q.correctIndex().equals(selected.get(q.id()))) {
                score += q.weight();
            }
        }
        String level = levelFor(score, max);
        this.userMapper.updateLevel(userId, level);
        return ApiResponse.ok(new PlacementDto.Result(level, score, max));
    }

    static String levelFor(int score, int max) {
        double ratio = (double) score / max;
        if (ratio < 0.25) return "A1";
        if (ratio < 0.5) return "A2";
        if (ratio < 0.7) return "B1";
        if (ratio < 0.9) return "B2";
        return "C1";
    }
}
