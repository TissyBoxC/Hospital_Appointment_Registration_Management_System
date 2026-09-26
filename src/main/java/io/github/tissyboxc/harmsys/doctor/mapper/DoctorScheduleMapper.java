package io.github.tissyboxc.harmsys.doctor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.tissyboxc.harmsys.doctor.entity.DoctorSchedule;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Param;

/** doctor_schedule 表数据访问。 */
public interface DoctorScheduleMapper extends BaseMapper<DoctorSchedule> {

  long countConflicts(
      @Param("doctorId") long doctorId,
      @Param("scheduleDate") LocalDate scheduleDate,
      @Param("period") int period,
      @Param("excludeScheduleId") Long excludeScheduleId);

  long countDoctorDepartment(
      @Param("doctorId") long doctorId, @Param("departmentId") long departmentId);

  int updateWithoutBookings(DoctorSchedule schedule);

  int deleteWithoutBookings(@Param("id") long id);
}
