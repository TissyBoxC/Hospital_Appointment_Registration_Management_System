package io.github.tissyboxc.harmsys.appointment.service.impl;

import io.github.tissyboxc.harmsys.appointment.service.AppointmentService;

import io.github.tissyboxc.harmsys.appointment.dto.CreateAppointmentRequest;
import io.github.tissyboxc.harmsys.appointment.entity.Appointment;
import io.github.tissyboxc.harmsys.appointment.mapper.AppointmentMapper;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.operationlog.mapper.OperationLogMapper;
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
public class AppointmentServiceImpl implements AppointmentService {
  private final AppointmentMapper mapper;
  private final OperationLogMapper operationLogMapper;

  public AppointmentServiceImpl(
      AppointmentMapper mapper, OperationLogMapper operationLogMapper) {
    this.mapper = mapper;
    this.operationLogMapper = operationLogMapper;
  }

  @Transactional(rollbackFor = Exception.class)
  /**
   * 创建预约时校验排班、号源和时间段，并以行锁保证并发一致性。
   */
  @Override
  public Map<String, Object> create(
      CreateAppointmentRequest request, HttpServletRequest httpRequest) {
    //验证身份
    AuthenticatedUser patient = requirePatient(httpRequest);
    //处理幂等请求
    if (request.request_no() != null && !request.request_no().isBlank()) {
      Map<String, Object> previous =
          mapper.selectIdempotentAppointment(
              request.request_no().trim(), patient.patient_id());
      if (previous != null)
        return detail(
            ((Number) previous.get("appointment_id")).longValue(), patient.patient_id(), null);
    }
    //锁定排班
    Map<String, Object> schedule = mapper.lockSchedule(request.schedule_id());
    if (schedule == null) throw new UserRegistrationException(404, "排班不存在");
    //校验排班状态和日期
    int scheduleStatus = ((Number) schedule.get("status")).intValue();
    if (scheduleStatus != 1
        || asLocalDate(schedule.get("schedule_date")).isBefore(java.time.LocalDate.now())) {
      throw new UserRegistrationException(409, "该排班当前不可预约");
    }
    //校验医生是否存在于系统
    long doctorId = ((Number) schedule.get("doctor_id")).longValue();
    if (mapper.countDoctorAvailable(doctorId) == 0) {
      throw new UserRegistrationException(409, "该医生当前不可预约");
    }
    //防止重复预约
    if (mapper.countPatientActiveAppointments(
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
          mapper.lockSlot(request.slot_id(), request.schedule_id());
      if (slot == null) throw new UserRegistrationException(404, "时间段不存在");
      if (((Number) slot.get("status")).intValue() != 0)
        throw new UserRegistrationException(409, "该时间段已被预约或锁定");
    }
    //生成队列号
    Integer nextQueueNo = mapper.nextQueueNo(request.schedule_id());
    int queueNo = nextQueueNo == null ? 1 : nextQueueNo;
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
          insertAppointment(
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
        && mapper.reserveSlot(request.slot_id())
            != 1) {
      throw new UserRegistrationException(409, "该时间段已被其他患者预约");
    }
    //增加该排版的预约数
    if (mapper.incrementBookedCount(request.schedule_id())
        != 1) {
      throw new UserRegistrationException(409, "该排班号源已满");
    }
    //创建支付记录
    createSuccessfulPayment(appointmentId, patient.patient_id(), fee);
    //写入幂等记录
    if (request.request_no() != null && !request.request_no().isBlank()) {
      mapper.insertIdempotency(
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
    Long doctorUserId = mapper.selectDoctorUserId(doctorId);
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
  @Override
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
  @Override
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
            ? mapper.lockAppointment(appointmentId)
            : mapper.lockPatientAppointment(appointmentId, patientId);
    if (appointment == null) throw new UserRegistrationException(404, "预约不存在或不属于当前患者");
    //校验预约状态
    int status = ((Number) appointment.get("status")).intValue();
    if (status != 1 && status != 2) throw new UserRegistrationException(409, "预约当前不能取消");
    //校验预约时间段
    java.time.LocalDate appointmentDate = asLocalDate(appointment.get("appointment_date"));
    if (appointmentDate != null
        && appointmentDate.isBefore(java.time.LocalDate.now()))
      throw new UserRegistrationException(409, "历史预约不能取消");

    if (appointmentDate != null
        && appointmentDate.equals(java.time.LocalDate.now())) {
      //查询预约开始时间
      Object slotTime =
          appointment.get("slot_id") == null
              ? null
              : mapper.selectSlotStartTime(((Number) appointment.get("slot_id")).longValue());
      Integer configuredCutoff = mapper.selectCancelCutoffMinutes();
      int cutoff = configuredCutoff == null ? 30 : configuredCutoff;
      //如果时间段开始时间 < 当前时间 + 30分钟,不允取消
      if (slotTime instanceof java.sql.Time t
          && t.toLocalTime().isBefore(java.time.LocalTime.now().plusMinutes(cutoff)))
        throw new UserRegistrationException(409, "已超过取消预约截止时间");
    }
    //获取排班ID
    long scheduleId = ((Number) appointment.get("schedule_id")).longValue();
    mapper.lockSchedule(scheduleId);
    Object slotId = appointment.get("slot_id");
    if (slotId != null
        && mapper.lockSlot(((Number) slotId).longValue(), scheduleId) == null)
      throw new UserRegistrationException(404, "时间段不存在");
    mapper.cancelAppointment(
        appointmentId, reason == null || reason.isBlank() ? "用户取消预约" : reason);
    if (slotId != null)
      //更新排班信息状态
      mapper.releaseSlot(((Number) slotId).longValue());
    mapper.decrementBookedCount(scheduleId);
    mapper.refundSuccessfulPayment(appointmentId);
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
  @Override
  public void checkIn(
      long appointmentId, Long patientId, long operatorId, String operation, String ip) {
    //获取预约信息
    Map<String, Object> appointment =
        patientId == null
            ? mapper.lockAppointment(appointmentId)
            : mapper.lockPatientAppointment(appointmentId, patientId);
    if (appointment == null) throw new UserRegistrationException(404, "预约不存在或不属于当前患者");
    //验证时间
    if (asLocalDate(appointment.get("appointment_date")).isAfter(java.time.LocalDate.now()))
      throw new UserRegistrationException(409, "未到就诊日期，不能签到");
    //验证预约状态
    if (((Number) appointment.get("status")).intValue() != 2)
      throw new UserRegistrationException(409, "预约当前不能签到");
    //状态置"3"已签到
    mapper.checkIn(appointmentId);
    //创建或复用就诊记录
    if (mapper.countVisit(appointmentId) == 0) {
      String visitNo =
          "V"
              + System.currentTimeMillis()
              + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
      mapper.insertVisit(
          appointmentId,
          appointment.get("patient_id"),
          appointment.get("doctor_id"),
          visitNo);
    }
    writeLog(operatorId, operation, "appointment", appointmentId, "预约签到", ip);
  }

  @Override
  public List<Map<String, Object>> patientList(HttpServletRequest request) {
    AuthenticatedUser p = requirePatient(request);
    return mapper.selectPatientAppointments(p.patient_id());
  }

  @Override
  public Map<String, Object> patientDetail(long id, HttpServletRequest request) {
    AuthenticatedUser p = requirePatient(request);
    return detail(id, p.patient_id(), null);
  }

  @Override
  public List<Map<String, Object>> doctorList(HttpServletRequest request) {
    AuthenticatedUser d = requireDoctor(request);
    return mapper.selectDoctorAppointments(d.doctor_id());
  }

  /**
   * 挂号员查询预约列表
   */
  @Override
  public List<Map<String, Object>> registrationList(HttpServletRequest request) {
    requireRegistration(request);
    return mapper.selectRegistrationAppointments();
  }

  /**
   * 挂号员查询预约队列
   */
  @Override
  public List<Map<String, Object>> queue(HttpServletRequest request) {
    requireRegistration(request);
    return mapper.selectQueue();
  }

  @Override
  public Map<String, Object> patientPage(
      HttpServletRequest request, int page, int size, Integer status) {
    AuthenticatedUser p = requirePatient(request);
    Pagination pagination = pagination(page, size);
    return page(
        pagination,
        mapper.selectPatientPage(
            p.patient_id(), status, pagination.size(), pagination.offset()),
        mapper.countPatientAppointments(p.patient_id(), status));
  }

  @Override
  public Map<String, Object> doctorPage(
      HttpServletRequest request, int page, int size, Integer status) {
    AuthenticatedUser d = requireDoctor(request);
    Pagination pagination = pagination(page, size);
    return page(
        pagination,
        mapper.selectDoctorPage(
            d.doctor_id(), status, pagination.size(), pagination.offset()),
        mapper.countDoctorAppointments(d.doctor_id(), status));
  }

  @Override
  public Map<String, Object> registrationPage(
      HttpServletRequest request, int page, int size, Integer status) {
    requireRegistration(request);
    Pagination pagination = pagination(page, size);
    return page(
        pagination,
        mapper.selectRegistrationPage(status, pagination.size(), pagination.offset()),
        mapper.countRegistrationAppointments(status));
  }

  /**
   * 用于返回更新后的预约信息
   * @param id
   * @param patientId
   * @param doctorId
   * @return
   */
  private Map<String, Object> detail(long id, Long patientId, Long doctorId) {
    Map<String, Object> result = mapper.selectDetail(id);
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
    mapper.insertSuccessfulPayment(no, appointmentId, patientId, fee);
  }

  private long insertAppointment(
      String appointmentNo,
      long patientId,
      long doctorId,
      Map<String, Object> schedule,
      long scheduleId,
      Long slotId,
      int queueNo,
      BigDecimal fee,
      String remark) {
    Appointment appointment = new Appointment();
    appointment.setAppointmentNo(appointmentNo);
    appointment.setPatientId(patientId);
    appointment.setDoctorId(doctorId);
    appointment.setDepartmentId(((Number) schedule.get("department_id")).longValue());
    appointment.setScheduleId(scheduleId);
    appointment.setSlotId(slotId);
    appointment.setAppointmentDate(asLocalDate(schedule.get("schedule_date")));
    appointment.setPeriod(((Number) schedule.get("period")).intValue());
    appointment.setQueueNo(queueNo);
    appointment.setFee(fee);
    appointment.setStatus(2);
    appointment.setRemark(remark);
    mapper.insert(appointment);
    return appointment.getId();
  }

  private Map<String, Object> page(
      Pagination pagination, List<Map<String, Object>> items, long total) {
    Map<String, Object> result = new java.util.LinkedHashMap<>();
    result.put("page", pagination.page());
    result.put("page_size", pagination.size());
    result.put("total", total);
    result.put("items", items);
    return result;
  }

  private java.time.LocalDate asLocalDate(Object value) {
    if (value == null) return null;
    if (value instanceof java.time.LocalDate localDate) return localDate;
    if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
    if (value instanceof java.util.Date date)
      return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    throw new IllegalArgumentException("无法转换排班日期: " + value);
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
    OperationLog log = new OperationLog();
    log.setUserId(userId);
    log.setOperationType(type);
    log.setTargetType(target);
    log.setTargetId(id);
    log.setDescription(desc);
    log.setIpAddress(ip);
    operationLogMapper.insert(log);
  }

  /**
   * 写入用户通知
   * @param userId 用户id
   * @param title 标题
   * @param content 内容
   * @param type 类型
   */
  private void notifyAccount(long userId, String title, String content, String type) {
    mapper.insertNotification(userId, title, content, type);
    String recipient = mapper.selectRecipientPhoneForUser(userId);
    if (recipient != null && !recipient.isBlank())
      mapper.insertOutbox(userId, "SMS", recipient, title, content);
  }

  private void notifyPatient(long patientId, String title, String content, String type) {
    Long userId = mapper.selectPatientUserId(patientId);
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

