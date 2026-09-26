package io.github.tissyboxc.harmsys.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.tissyboxc.harmsys.payment.entity.PaymentRecord;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** payment_record 表及支付流程数据访问。 */
public interface PaymentMapper extends BaseMapper<PaymentRecord> {

  Map<String, Object> selectMapById(@Param("id") long id);

  Map<String, Object> lockPayment(@Param("paymentId") long paymentId);

  Map<String, Object> lockAppointmentForPatient(
      @Param("appointmentId") long appointmentId, @Param("patientId") long patientId);

  Map<String, Object> lockAppointment(@Param("appointmentId") long appointmentId);

  Map<String, Object> selectLatestAppointmentPayment(@Param("appointmentId") long appointmentId);

  int insertSuccessfulPayment(
      @Param("paymentNo") String paymentNo,
      @Param("appointmentId") long appointmentId,
      @Param("patientId") long patientId,
      @Param("amount") Object amount,
      @Param("thirdPartyNo") String thirdPartyNo);

  Map<String, Object> selectByPaymentNo(@Param("paymentNo") String paymentNo);

  int markAppointmentPaid(@Param("appointmentId") long appointmentId);

  int lockSchedule(@Param("scheduleId") long scheduleId);

  int lockSlot(@Param("slotId") long slotId);

  int markPaymentRefunded(
      @Param("paymentId") long paymentId,
      @Param("reason") String reason,
      @Param("operatorId") long operatorId);

  int cancelAppointment(@Param("appointmentId") long appointmentId, @Param("reason") String reason);

  int releaseSlot(@Param("slotId") long slotId);

  int decrementBookedCount(@Param("scheduleId") long scheduleId);

  Long selectPatientUserId(@Param("patientId") long patientId);

  int insertNotification(
      @Param("userId") long userId,
      @Param("title") String title,
      @Param("content") String content,
      @Param("notificationType") String notificationType);

  String selectPatientPhone(@Param("patientId") long patientId);

  int insertNotificationOutbox(
      @Param("userId") long userId,
      @Param("channel") String channel,
      @Param("recipient") String recipient,
      @Param("subject") String subject,
      @Param("content") String content);
}
