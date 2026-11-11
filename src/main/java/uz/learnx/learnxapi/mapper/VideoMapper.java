package uz.learnx.learnxapi.mapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import uz.learnx.learnxapi.dto.LearningDto;
import uz.learnx.learnxapi.dto.VideoDto;

@Mapper
public interface VideoMapper {

    List<VideoDto.VideoSummary> list(@Param("userId") UUID userId);

    Optional<VideoDto.VideoSummary> find(@Param("userId") UUID userId, @Param("videoId") UUID videoId);

    List<LearningDto.Phrase> phrases(@Param("videoId") UUID videoId);

    /** @return 1 the first time a video is completed, otherwise 0 */
    int markCompleted(@Param("userId") UUID userId, @Param("videoId") UUID videoId, @Param("at") Instant at);
}
