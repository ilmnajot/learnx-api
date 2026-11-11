package uz.learnx.learnxapi.mapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import uz.learnx.learnxapi.dto.CommentDto;

@Mapper
public interface CommentMapper {

    void upsert(@Param("userId") UUID userId, @Param("body") String body, @Param("rating") int rating);

    Optional<CommentDto.OwnComment> own(@Param("userId") UUID userId);

    int deleteOwn(@Param("userId") UUID userId);

    List<CommentDto.PublicComment> approved(@Param("limit") int limit);

    List<CommentDto.AdminComment> forModeration(@Param("status") String status);

    int setStatus(@Param("id") UUID id, @Param("status") String status, @Param("at") Instant at);

    int delete(@Param("id") UUID id);
}
