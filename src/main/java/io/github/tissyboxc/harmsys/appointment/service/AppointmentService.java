package io.github.tissyboxc.harmsys.appointment.service;

import io.github.tissyboxc.harmsys.appointment.dto.CreateAppointmentRequest;
import io.github.tissyboxc.harmsys.appointment.repository.AppointmentRepository;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * 处理预约创建、取消、签到及相应支付和通知。
 */
public class AppointmentService {
  private final AppointmentRepository repository;

  public AppointmentService(AppointmentRepository repository) {
    this.repository = repository;
  }

  @Transactional(rollbackFor = Exception.class)
  /**
   * 创建预约时校验排班、号源和时间段，并以行锁保证并发一致性。
   */
  public Map<String, Object> create(
      CreateAppointmentRequest request, HttpServletRequest httpRequest) {
    //验证身份
    AuthenticatedUser patient = requirePatient(httpRequest);
    //处理幂等请求
    if (request.request_no() != null && !request.request_no().isBlank()) {
      Map<String, Object> previous =
          repository.findIdempotentAppointment(
              request.request_no().trim(), patient.patient_id());
      if (previous != null)
        return detail(
            ((Number) previous.get("appointment_id")).longValue(), patient.patient_id(), null);
    }
    //锁定排班
    Map<String, Object> schedule = repository.lockSchedule(request.schedule_id());
    if (schedule == null) throw new UserRegistrationException(404, "排班不存在");
    //校验排班状态和日期
    int scheduleStatus = ((Number) schedule.get("status")).intValue();
    if (scheduleStatus != 1
        || ((java.sql.Date) schedule.get("schedule_date"))
            .toLocalDate()
            .isBefore(java.time.LocalDate.now())) {
      throw new UserRegistrationException(409, "该排班当前不可预约");
    }
    //校验医生是否存在于系统
    long doctorId = ((Number) schedule.get("doctor_id")).longValue();
    if (!repository.doctorAvailable(doctorId)) {
      throw new UserRegistrationException(409, "该医生当前不可预约");
    }
    //防止重复预约
    if (repository.countPatientActiveAppointments(
            patient.patient_id(), request.schedule_id())
        > 0) {
      throw new UserRegistrationException(409, "您已经预约过该排班");
    }
    //校验剩余号源
    if (((Number) schedule.get("booked_count")).intValue()
        >= ((Number) schedule.get("total_count")).intValue()) {
      throw new UserRegistrationException(409, "该排班号源已满");
    }
    //校验时间段,同时校验时间段存在,状态为0,所选时间段为指定排班
    if (request.slot_id() != null) {
      Map<String, Object> slot =
          repository.lockSlot(request.slot_id(), request.schedule_id());
      if (slot == null) throw new UserRegistrationException(404, "时间段不存在");
      if (((Number) slot.get("status")).intValue() != 0)
        throw new UserRegistrationException(409, "该时间段已被预约或锁定");
    }
    //生成队列号
    int queueNo = repository.nextQueueNo(request.schedule_id());
    //生产预约号
    String appointmentNo =
        "A"
            + System.currentTimeMillis()
            + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    //在排班表中读取挂号费
    BigDecimal fee = (BigDecimal) schedule.get("fee");
    long appointmentId;
    try {
      appointmentId =
          repository.insertAppointment(
              appointmentNo,
              patient.patient_id(),
              doctorId,
              schedule,
              request.schedule_id(),
              request.slot_id(),
              queueNo,
              fee,
              request.remark());
    }
    //触发数据库唯一异常
    catch (DuplicateKeyException ex) {
      throw new UserRegistrationException(409, "该时间段已被其他患者预约");
    }
    //占用时间段
    if (request.slot_id() != null
        && repository.reserveSlot(request.slot_id())
            != 1) {
      throw new UserRegistrationException(409, "该时间段已被其他患者预约");
    }
    //增加该排版的预约数
    if (repository.incrementBookedCount(request.schedule_id())
        != 1) {
      throw new UserRegistrationException(409, "该排班号源已满");
    }
    //创建支付记录
    createSuccessfulPayment(appointmentId, patient.patient_id(), fee);
    //写入幂等记录
    if (request.request_no() != null && !request.request_no().isBlank()) {
      repository.insertIdempotency(
          request.request_no().trim(), patient.patient_id(), appointmentId);
    }
    //写日志
    writeLog(
        patient.user_id(),
        "CREATE_APPOINTMENT",
        "appointment",
        appointmentId,
        "患者创建预约并自动支付成功",
        httpRequest.getRemoteAddr());
    //写通知
    notifyAccount(
        patient.user_id(), "预约成功", "预约 " + appointmentNo + " 已创建并完成模拟支付", "APPOINTMENT_CREATED");
    Long doctorUserId = repository.doctorUserId(doctorId);
    if (doctorUserId != null)
      notifyAccount(
          doctorUserId,
          "新预约提醒",
          "患者预约了您的 " + schedule.get("schedule_date") + " 出诊",
          "APPOINTMENT_CREATED");
    return detail(appointmentId, patient.patient_id(), null);
  }

  @Transactional(rollbackFor = Exception.class)
  /** 患者取消自己的预约，并释放号源、退款和发送通知。 */
  public void cancelPatient(long appointmentId, String reason, HttpServletRequest request) {
    AuthenticatedUser patient = requirePatient(request);
    cancel(
        appointmentId,
        patient.patient_id(),
        "PATIENT_CANCEL_APPOINTMENT",
        reason,
        patient.user_id(),
        request.getRemoteAddr());
  }

  @Transactional(rollbackFor = Exception.class)
  /** 挂号员取消预约，不限制预约所属患者。 */
  public void cancelByRegistration(long appointmentId, String reason, HttpServletRequest request) {
    AuthenticatedUser operator = requireRegistration(request);
    cancel(
        appointmentId,
        null,
        "REGISTRATION_CANCEL_APPOINTMENT",
        reason,
        operator.user_id(),
        request.getRemoteAddr());
  }

  /**
   *统一取消逻辑
   * @param appointmentId 预约id
   * @param patientId 患者id
   * @param operation 操作者
   * @param reason 原因
   * @param operatorId 记录号
   * @param ip 地址
   */
  private void cancel(
      long appointmentId,
      Long patientId,
      String operation,
      String reason,
      long operatorId,
      String ip) {
    //取得当前预约信息
    Map<String, Object> appointment =
        patientId == null
            ? repository.lockAppointment(appointmentId)
            : repository.lockPatientAppointment(appointmentId, patientId);
    if (appointment == null) throw new UserRegistrationException(404, "预约不存在或不属于当前患者");
    //校验预约状态
    int status = ((Number) appointment.get("status")).intValue();
    if (status != 1 && status != 2) throw new UserRegistrationException(409, "预约当前不能取消");
    //校验预约时间段
    java.sql.Date appointmentDate = (java.sql.Date) appointment.get("appointment_date");
    if (appointmentDate != null
        && appointmentDate.toLocalDate().isBefore(java.time.LocalDate.now()))
      throw new UserRegistrationException(409, "历史预约不能取消");

    if (appointmentDate != null
        && appointmentDate.toLocalDate().equals(java.time.LocalDate.now())) {
      //查询预约开始时间
      Object slotTime =
          appointment.get("slot_id") == null
              ? null
              : repository.slotStartTime(((Number) appointment.get("slot_id")).longValue());
      int cutoff = repository.cancelCutoffMinutes();
      //如果时间段开始时间 < 当前时间 + 30分钟,不允取消
      if (slotTime instanceof java.sql.Time t
          && t.toLocalTime().isBefore(java.time.LocalTime.now().plusMinutes(cutoff)))
        throw new UserRegistrationException(409, "已超过取消预约截止时间");
    }
    //获取排班ID
    long scheduleId = ((Number) appointment.get("schedule_id")).longValue();
    repository.lockSchedule(scheduleId);
    Object slotId = appointment.get("slot_id");
    if (slotId != null
        && repository.lockSlot(((Number) slotId).longValue(), scheduleId) == null)
      throw new UserRegistrationException(404, "时间段不存在");
    repository.cancelAppointment(
        appointmentId, reason == null || reason.isBlank() ? "用户取消预约" : reason);
    if (slotId != null)
      //更新排班信息状态
      repository.releaseSlot(((Number) slotId).longValue());
    repository.decrementBookedCount(scheduleId);
    repository.refundSuccessfulPayment(appointmentId);
    //写日志
    writeLog(operatorId, operation, "appointment", appointmentId, "取消预约并自动退款", ip);
    //写通知
    notifyPatient(
        ((Number) appointment.get("patient_id")).longValue(),
        "预约已取消",
        "预约已取消，退款已按模拟流程完成",
        "APPOINTMENT_CANCELLED");
  }

  /**
   * 签到统一逻辑
   * @param appointmentId 预约ID
   * @param patientId 患者ID
   * @param operatorId 操作者ID
   * @param operation 操作
   * @param ip 地址
   */
  @Transactional(rollbackFor = Exception.class)
  public void checkIn(
      long appointmentId, Long patientId, long operatorId, String operation, String ip) {
    //获取预约信息
    Map<String, Object> appointment =
        patientId == null
            ? repository.lockAppointment(appointmentId)
            : repository.lockPatientAppointment(appointmentId, patientId);
    if (appointment == null) throw new UserRegistrationException(404, "预约不存在或不属于当前患者");
    //验证时间
    if (((java.sql.Date) appointment.get("appointment_date"))
        .toLocalDate()
        .isAfter(java.time.LocalDate.now()))
      throw new UserRegistrationException(409, "未到就诊日期，不能签到");
    //验证预约状态
    if (((Number) appointment.get("status")).intValue() != 2)
      throw new UserRegistrationException(409, "预约当前不能签到");
    //状态置"3"已签到
    repository.checkIn(appointmentId);
    //创建或复用就诊记录
    if (!repository.visitExists(appointmentId)) {
      String visitNo =
          "V"
              + System.currentTimeMillis()
              + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
      repository.insertVisit(
          appointmentId,
          appointment.get("patient_id"),
          appointment.get("doctor_id"),
          visitNo);
    }
    writeLog(operatorId, operation, "appointment", appointmentId, "预约签到", ip);
  }

  public List<Map<String, Object>> patientList(HttpServletRequest request) {
    AuthenticatedUser p = requirePatient(request);
    return repository.patientAppointments(p.patient_id());
  }

  public Map<String, Object> patientDetail(long id, HttpServletRequest request) {
    AuthenticatedUser p = requirePatient(request);
    return detail(id, p.patient_id(), null);
  }

  public List<Map<String, Object>> doctorList(HttpServletRequest request) {
    AuthenticatedUser d = requireDoctor(request);
    return repository.doctorAppointments(d.doctor_id());
  }

  /**
   * 挂号员查询预约列表
   */
  public List<Map<String, Object>> registrationList(HttpServletRequest request) {
    requireRegistration(request);
    return repository.registrationAppointments();
  }

  /**
   * 挂号员查询预约队列
   */
  public List<Map<String, Object>> queue(HttpServletRequest request) {
    requireRegistration(request);
    return repository.queue();
  }

  public Map<String, Object> patientPage(
      HttpServletRequest request, int page, int size, Integer status) {
    AuthenticatedUser p = requirePatient(request);
    Pagination pagination = pagination(page, size);
    return repository.patientPage(p.patient_id(), status, pagination.size(), pagination.offset());
  }

  public Map<String, Object> doctorPage(
      HttpServletRequest request, int page, int size, Integer status) {
    AuthenticatedUser d = requireDoctor(request);
    Pagination pagination = pagination(page, size);
    return repository.doctorPage(d.doctor_id(), status, pagination.size(), pagination.offset());
  }

  public Map<String, Object> registrationPage(
      HttpServletRequest request, int page, int size, Integer status) {
    requireRegistration(request);
    Pagination pagination = pagination(page, size);
    return repository.registrationPage(status, pagination.size(), pagination.offset());
  }

  /**
   * 用于返回更新后的预约信息
   * @param id
   * @param patientId
   * @param doctorId
   * @return
   */
  private Map<String, Object> detail(long id, Long patientId, Long doctorId) {
    Map<String, Object> result = repository.detail(id);
    if (result == null
        || (patientId != null && !patientId.equals(((Number) result.get("patient_id")).longValue()))
        || (doctorId != null && !doctorId.equals(((Number) result.get("doctor_id")).longValue())))
      throw new UserRegistrationException(404, "预约不存在或无权访问");
    return result;
  }

  /**
   * 锁定排班
   * @param id
   * @return
   */
  /**
   * 创建支付记录,直接成功
   * @param appointmentId 预约id
   * @param patientId 患者id
   * @param fee 费用
   */
  private void createSuccessfulPayment(long appointmentId, long patientId, BigDecimal fee) {
    String no =
        "PAY"
            + System.currentTimeMillis()
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    repository.insertSuccessfulPayment(no, appointmentId, patientId, fee);
  }

  private Pagination pagination(int page, int size) {
    int currentPage = Math.max(1, page);
    int pageSize = Math.min(Math.max(1, size), 100);
    return new Pagination(currentPage, pageSize, (currentPage - 1) * pageSize);
  }

  /**
   * 写入日志
   * @param userId
   * @param type
   * @param target
   * @param id
   * @param desc
   * @param ip
   */
  private void writeLog(long userId, String type, String target, long id, String desc, String ip) {
    repository.insertOperationLog(userId, type, target, id, desc, ip);
  }

  /**
   * 写入用户通知
   * @param userId 用户id
   * @param title 标题
   * @param content 内容
   * @param type 类型
   */
  private void notifyAccount(long userId, String title, String content, String type) {
    repository.insertNotification(userId, title, content, type);
    String recipient = repository.recipientPhoneForUser(userId);
    if (recipient != null && !recipient.isBlank())
      repository.insertOutbox(userId, "SMS", recipient, title, content);
  }

  private void notifyPatient(long patientId, String title, String content, String type) {
    Long userId = repository.patientUserId(patientId);
    if (userId == null) throw new UserRegistrationException(404, "患者账号不存在");
    notifyAccount(userId, title, content, type);
  }

  /**
   * 验证当前账号必须是患者
   * @param r
   * @return
   */
  private AuthenticatedUser requirePatient(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.patient_id() == null
        || u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("PATIENT")))
      throw new SessionAuthenticationException(403, "当前账号不是患者");
    return u;
  }

  /**
   * 验证当前账号必须是医生
   * @param r
   * @return
   */
  private AuthenticatedUser requireDoctor(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.doctor_id() == null
        || u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("DOCTOR")))
      throw new SessionAuthenticationException(403, "当前账号不是医生");
    return u;
  }

  /**
   * 验证当前账号必须是挂号员
   * @param r
   * @return
   */
  private AuthenticatedUser requireRegistration(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.role_codes().stream()
        .noneMatch(x -> x.equalsIgnoreCase("REGISTRATION") || x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "没有挂号员权限");
    return u;
  }

  private record Pagination(int page, int size, int offset) {}
}

