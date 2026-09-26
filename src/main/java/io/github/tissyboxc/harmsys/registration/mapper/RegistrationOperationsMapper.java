package io.github.tissyboxc.harmsys.registration.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 挂号员代挂号、退款和队列操作的数据访问。 */
public interface RegistrationOperationsMapper {

  List<Map<String, Object>> selectPatients(@Param("keyword") String keyword);

  Map<String, Object> selectPatient(@Param("id") long id);

  long countActivePatient(@Param("id") long id);

  Map<String, Object> lockSchedule(@Param("id") long id);

  int reserveSlot(@Param("slotId") long slotId, @Param("scheduleId") long scheduleId);

  Integer selectNextQueueNo(@Param("scheduleId") long scheduleId);

  int insertAppointment(
      @Param("appointmentNo") String appointmentNo,
      @Param("patientId") long patientId,
      @Param("doctorId") Object doctorId,
      @Param("departmentId") Object departmentId,
      @Param("scheduleId") long scheduleId,
      @Param("slotId") Long slotId,
      @Param("appointmentDate") Object appointmentDate,
      @Param("period") Object period,
      @Param("queueNo") int queueNo,
      @Param("fee") Object fee,
      @Param("remark") String remark);

  Long selectAppointmentIdByNo(@Param("appointmentNo") String appointmentNo);

  int incrementBookedCount(@Param("scheduleId") long scheduleId);

  int insertSuccessfulPayment(
      @Param("paymentNo") String paymentNo,
      @Param("appointmentId") long appointmentId,
      @Param("patientId") long patientId,
      @Param("amount") BigDecimal amount);

  Map<String, Object> lockAppointment(@Param("id") long id);

  int refundPayment(@Param("appointmentId") long appointmentId);

  int markRefundedAppointment(@Param("id") long id);

  int releaseSlot(@Param("slotId") long slotId);

  int decrementBookedCount(@Param("scheduleId") long scheduleId);

  int callNext(@Param("appointmentId") long appointmentId);

  int startVisitForAppointment(@Param("appointmentId") long appointmentId);

  int markNoShow(@Param("appointmentId") long appointmentId);

  int requeue(@Param("appointmentId") long appointmentId);

  Map<String, Object> selectAppointment(@Param("id") long id);

  Map<String, Object> selectAppointmentDetail(@Param("id") long id);
}
