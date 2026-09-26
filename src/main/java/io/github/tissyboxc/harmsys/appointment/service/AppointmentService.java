package io.github.tissyboxc.harmsys.appointment.service;

import io.github.tissyboxc.harmsys.appointment.dto.CreateAppointmentRequest;
import io.github.tissyboxc.harmsys.appointment.entity.Appointment;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 处理预约创建、取消、签到及相应支付和通知。
 */
public interface AppointmentService {
public Map<String, Object> create(CreateAppointmentRequest request, HttpServletRequest httpRequest);

  public void cancelPatient(long appointmentId, String reason, HttpServletRequest request);

  public void cancelByRegistration(long appointmentId, String reason, HttpServletRequest request);

  public void checkIn(long appointmentId, Long patientId, long operatorId, String operation, String ip);

  public List<Map<String, Object>> patientList(HttpServletRequest request);

  public Map<String, Object> patientDetail(long id, HttpServletRequest request);

  public List<Map<String, Object>> doctorList(HttpServletRequest request);

  public List<Map<String, Object>> registrationList(HttpServletRequest request);

  public List<Map<String, Object>> queue(HttpServletRequest request);

  public Map<String, Object> patientPage(HttpServletRequest request, int page, int size, Integer status);

  public Map<String, Object> doctorPage(HttpServletRequest request, int page, int size, Integer status);

  public Map<String, Object> registrationPage(HttpServletRequest request, int page, int size, Integer status);
}
