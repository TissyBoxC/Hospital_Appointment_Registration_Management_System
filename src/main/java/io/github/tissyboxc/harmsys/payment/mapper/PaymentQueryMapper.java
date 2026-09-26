package io.github.tissyboxc.harmsys.payment.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 支付记录分页与联表查询。 */
public interface PaymentQueryMapper {

  List<Map<String, Object>> selectPatientPayments(
      @Param("patientId") long patientId,
      @Param("size") int size,
      @Param("offset") int offset);

  long countPatientPayments(@Param("patientId") long patientId);

  Map<String, Object> selectAppointmentPayment(
      @Param("appointmentId") long appointmentId, @Param("patientId") long patientId);

  List<Map<String, Object>> selectAdminPayments(
      @Param("status") Integer status,
      @Param("size") int size,
      @Param("offset") int offset);

  long countAdminPayments(@Param("status") Integer status);
}
