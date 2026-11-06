package uz.learnx.learnxapi.placement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.learnx.learnxapi.common.error.ApiException;
import uz.learnx.learnxapi.user.UserMapper;

@Service
public class PlacementService {

    @Mapper
    public interface PlacementMapper {
        @Select("SELECT id, prompt, options, correct_index, weight FROM placement_questions ORDER BY position")
        List<Question> questions();
    }

    public record Question(UUID id, String prompt, List<String> options, Integer correctIndex, Integer weight) {}

    public record Result(String level, int score, int maxScore) {}

    private final PlacementMapper placement;
    private final UserMapper users;

    PlacementService(PlacementMapper placement, UserMapper users) {
        this.placement = placement;
        this.users = users;
    }

    public List<Question> questions() {
        return placement.questions();
    }

    /** @param answers selected option index per question id; unknown question ids are ignored */
    @Transactional
    public Result submit(UUID userId, Map<UUID, Integer> answers) {
        List<Question> questions = placement.questions();
        if (questions.isEmpty()) {
            throw ApiException.notFound("Placement test");
        }
        Map<UUID, Integer> selected = new HashMap<>(answers);
        int max = 0;
        int score = 0;
        for (Question q : questions) {
            max += q.weight();
            if (q.correctIndex().equals(selected.get(q.id()))) {
                score += q.weight();
            }
        }
        String level = levelFor(score, max);
        users.updateLevel(userId, level);
        return new Result(level, score, max);
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
