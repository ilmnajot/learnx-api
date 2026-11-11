package uz.learnx.learnxapi.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import uz.learnx.learnxapi.dto.PlacementDto;

@Mapper
public interface PlacementMapper {

    List<PlacementDto.Question> questions();
}
