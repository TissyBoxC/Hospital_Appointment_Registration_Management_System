package io.github.tissyboxc.harmsys.doctor.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 排班停诊、时间段维护相关的数据访问。 */
public interface ScheduleLifecycleMapper {

  Map<String, Object> lockSchedule(@Param("id") long id);

  Map<String, Object> lockDoctorSchedule(
      @Param("id") long id, @Param("doctorId") long doctorId);

  Map<String, Object> findSchedule(@Param("id") long id);

  Map<String, Object> findSlot(@Param("slotId") long slotId);

  int updateScheduleStatus(@Param("id") long id, @Param("status") int status);

  int updateSlotStatus(@Param("slotId") long slotId, @Param("status") int status);

  int deleteSlot(@Param("slotId") long slotId);

  long countBlockingAppointments(@Param("slotId") long slotId);

  int stopSchedule(@Param("id") long id, @Param("reason") String reason);

  List<Map<String, Object>> activeAppointments(@Param("scheduleId") long scheduleId);

  int cancelAppointment(@Param("id") long id, @Param("reason") String reason);

  int releaseSlot(@Param("slotId") long slotId);

  int refundSuccessfulPayment(@Param("appointmentId") long appointmentId);

  int resetBookedCount(@Param("scheduleId") long scheduleId);
}
