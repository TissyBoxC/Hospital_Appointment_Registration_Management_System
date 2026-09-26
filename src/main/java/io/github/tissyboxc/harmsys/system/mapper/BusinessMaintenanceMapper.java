package io.github.tissyboxc.harmsys.system.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 后台业务状态维护的数据访问。 */
public interface BusinessMaintenanceMapper {

  int activateDueSchedules();

  int closeExpiredSchedules();

  List<Map<String, Object>> selectExpiredAppointments();

  int expireAppointment(@Param("appointmentId") long appointmentId);

  int releaseSlot(@Param("slotId") long slotId);

  int decrementBookedCount(@Param("scheduleId") long scheduleId);

  int markNoShows();

  int reconcileBookedCounts();

  int repairOrphanSlots();

  int reconcilePayments();
}
