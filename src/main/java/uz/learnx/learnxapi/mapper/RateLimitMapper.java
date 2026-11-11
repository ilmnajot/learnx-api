package uz.learnx.learnxapi.mapper;

import java.time.Instant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RateLimitMapper {

    int hit(@Param("bucket") String bucket, @Param("windowStart") Instant windowStart);

    int deleteOlderThan(@Param("before") Instant before);
}
