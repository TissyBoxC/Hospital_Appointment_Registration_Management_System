package io.github.tissyboxc.harmsys.appointment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.tissyboxc.harmsys.appointment.entity.Appointment;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** appointment 表及预约事务相关数据访问。 */
public interface AppointmentMapper extends BaseMapper<Appointment> {

  Map<String, Object> selectIdempotentAppointment(
      @Param("requestNo") String requestNo, @Param("patientId") long patientId);

  Map<String, Object> lockSchedule(@Param("scheduleId") long scheduleId);

  Map<String, Object> lockSlot(
      @Param("slotId") long slotId, @Param("scheduleId") long scheduleId);

  long countDoctorAvailable(@Param("doctorId") long doctorId);

  long countPatientActiveAppointments(
      @Param("patientId") long patientId, @Param("scheduleId") long scheduleId);

  Integer nextQueueNo(@Param("scheduleId") long scheduleId);

  int reserveSlot(@Param("slotId") long slotId);

  int releaseSlot(@Param("slotId") long slotId);

  int incrementBookedCount(@Param("scheduleId") long scheduleId);

  int decrementBookedCount(@Param("scheduleId") long scheduleId);

  int insertIdempotency(
      @Param("requestNo") String requestNo,
      @Param("patientId") long patientId,
      @Param("appointmentId") long appointmentId);

  int insertSuccessfulPayment(
      @Param("paymentNo") String paymentNo,
      @Param("appointmentId") long appointmentId,
      @Param("patientId") long patientId,
      @Param("fee") java.math.BigDecimal fee);

  int insertNotification(
      @Param("userId") long userId,
      @Param("title") String title,
      @Param("content") String content,
      @Param("notificationType") String notificationType);

  String selectRecipientPhoneForUser(@Param("userId") long userId);

  int insertOutbox(
      @Param("userId") long userId,
      @Param("channel") String channel,
      @Param("recipient") String recipient,
      @Param("subject") String subject,
      @Param("content") String content);

  Long selectDoctorUserId(@Param("doctorId") long doctorId);

  Map<String, Object> selectDetail(@Param("appointmentId") long appointmentId);

  Map<String, Object> lockAppointment(@Param("id") long id);

  Map<String, Object> lockPatientAppointment(
      @Param("id") long id, @Param("patientId") long patientId);

  java.sql.Time selectSlotStartTime(@Param("slotId") long slotId);

  Integer selectCancelCutoffMinutes();

  int cancelAppointment(@Param("id") long id, @Param("reason") String reason);

  int refundSuccessfulPayment(@Param("appointmentId") long appointmentId);

  int checkIn(@Param("appointmentId") long appointmentId);

  long countVisit(@Param("appointmentId") long appointmentId);

  int insertVisit(
      @Param("appointmentId") long appointmentId,
      @Param("patientId") Object patientId,
      @Param("doctorId") Object doctorId,
      @Param("visitNo") String visitNo);

  List<Map<String, Object>> selectPatientAppointments(@Param("patientId") long patientId);

  List<Map<String, Object>> selectAllForAdmin();

  List<Map<String, Object>> selectAdminPage(
      @Param("status") Integer status,
      @Param("size") int size,
      @Param("offset") int offset);

  long countAdminAppointments(@Param("status") Integer status);

  Map<String, Object> selectAdminDetail(@Param("id") long id);

  long countActiveForPatientAndSchedule(
      @Param("patientId") long patientId,
      @Param("scheduleId") long scheduleId,
      @Param("exceptId") long exceptId);

  int updateAppointment(
      @Param("id") long id,
      @Param("queueNo") Integer queueNo,
      @Param("status") int status,
      @Param("remark") String remark);

  int migrateAppointment(
      @Param("id") long id,
      @Param("doctorId") Object doctorId,
      @Param("departmentId") Object departmentId,
      @Param("scheduleId") long scheduleId,
      @Param("slotId") Long slotId,
      @Param("appointmentDate") Object appointmentDate,
      @Param("period") Object period,
      @Param("fee") Object fee);

  int refundSuccessfulPaymentWithReason(
      @Param("appointmentId") long appointmentId, @Param("reason") String reason);

  int cancelByAdmin(@Param("id") long id, @Param("reason") String reason);

  int expire(@Param("id") long id);

  int incrementBookedCountUnchecked(@Param("scheduleId") long scheduleId);

  List<Map<String, Object>> selectDoctorAppointments(@Param("doctorId") long doctorId);

  List<Map<String, Object>> selectRegistrationAppointments();

  List<Map<String, Object>> selectQueue();

  List<Map<String, Object>> selectPatientPage(
      @Param("patientId") long patientId,
      @Param("status") Integer status,
      @Param("size") int size,
      @Param("offset") int offset);

  long countPatientAppointments(
      @Param("patientId") long patientId, @Param("status") Integer status);

  List<Map<String, Object>> selectDoctorPage(
      @Param("doctorId") long doctorId,
      @Param("status") Integer status,
      @Param("size") int size,
      @Param("offset") int offset);

  long countDoctorAppointments(
      @Param("doctorId") long doctorId, @Param("status") Integer status);

  List<Map<String, Object>> selectRegistrationPage(
      @Param("status") Integer status,
      @Param("size") int size,
      @Param("offset") int offset);

  long countRegistrationAppointments(@Param("status") Integer status);

  Long selectPatientUserId(@Param("patientId") long patientId);
}
