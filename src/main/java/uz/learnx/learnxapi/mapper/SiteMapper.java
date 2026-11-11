package uz.learnx.learnxapi.mapper;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import uz.learnx.learnxapi.dto.SiteDto;

@Mapper
public interface SiteMapper {

    List<SiteDto.SettingRow> allSettings();

    void putSetting(@Param("key") String key, @Param("value") String value);

    SiteDto.Totals publicTotals(@Param("weekAgo") LocalDate weekAgo);

    List<SiteDto.LevelCount> publicLevels();
}
