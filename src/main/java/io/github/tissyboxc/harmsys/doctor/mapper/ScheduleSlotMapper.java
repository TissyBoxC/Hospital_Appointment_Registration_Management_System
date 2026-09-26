package io.github.tissyboxc.harmsys.doctor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.tissyboxc.harmsys.doctor.entity.ScheduleSlot;
import java.time.LocalTime;
import org.apache.ibatis.annotations.Param;

/** schedule_slot 表数据访问。 */
public interface ScheduleSlotMapper extends BaseMapper<ScheduleSlot> {

  long countOverlaps(
      @Param("scheduleId") long scheduleId,
      @Param("startTime") LocalTime startTime,
      @Param("endTime") LocalTime endTime);
}
