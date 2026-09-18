package io.github.tissyboxc.harmsys.appointment;

import io.github.tissyboxc.harmsys.appointment.dto.CreateAppointmentRequest;
import io.github.tissyboxc.harmsys.users.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.users.UserRegistrationException;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * 处理预约创建、取消、签到及相应支付和通知。
 */
public class AppointmentService {
  private final JdbcTemplate jdbc;

  public AppointmentService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
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
          one(
              "SELECT appointment_id " +
                   "FROM appointment_idempotency " +
                   "WHERE request_no=? AND patient_id=?",
              request.request_no().trim(),
              patient.patient_id());
      if (previous != null)
        return detail(
            ((Number) previous.get("appointment_id")).longValue(), patient.patient_id(), null);
    }
    //锁定排班
    Map<String, Object> schedule = lockSchedule(request.schedule_id());
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
    if (count("SELECT COUNT(*) FROM doctor WHERE id=? AND status=1 AND deleted=0", doctorId) == 0) {
      throw new UserRegistrationException(409, "该医生当前不可预约");
    }
    //防止重复预约
    if (count(
            "SELECT COUNT(*) FROM appointment WHERE patient_id=? AND schedule_id=? AND status IN"
                + " (1,2,3,4)",
            patient.patient_id(),
            request.schedule_id())
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
      Map<String, Object> slot = lockSlot(request.slot_id(), request.schedule_id());
      if (((Number) slot.get("status")).intValue() != 0)
        throw new UserRegistrationException(409, "该时间段已被预约或锁定");
    }
    //生成队列号
    int queueNo = nextQueueNo(request.schedule_id());
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
              appointmentNo, patient.patient_id(), doctorId, schedule, request, queueNo, fee);
    }
    //触发数据库唯一异常
    catch (DuplicateKeyException ex) {
      throw new UserRegistrationException(409, "该时间段已被其他患者预约");
    }
    //占用时间段
    if (request.slot_id() != null
        && jdbc.update(
                "UPDATE schedule_slot SET status=1 WHERE id=? AND status=0", request.slot_id())
            != 1) {
      throw new UserRegistrationException(409, "该时间段已被其他患者预约");
    }
    //增加该排版的预约数
    if (jdbc.update(
            "UPDATE doctor_schedule SET booked_count=booked_count+1 WHERE id=? AND booked_count <"
                + " total_count",
            request.schedule_id())
        != 1) {
      throw new UserRegistrationException(409, "该排班号源已满");
    }
    //创建支付记录
    createSuccessfulPayment(appointmentId, patient.patient_id(), fee);
    //写入幂等记录
    if (request.request_no() != null && !request.request_no().isBlank()) {
      jdbc.update(
          "INSERT INTO appointment_idempotency(request_no,patient_id,appointment_id) VALUES(?,?,?)",
          request.request_no().trim(),
          patient.patient_id(),
          appointmentId);
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
    notifyUser(
        patient.user_id(), "预约成功", "预约 " + appointmentNo + " 已创建并完成模拟支付", "APPOINTMENT_CREATED");
    Long doctorUserId =
        jdbc.query(
            "SELECT user_id FROM doctor WHERE id=?",
            rs -> rs.next() ? rs.getLong(1) : null,
            doctorId);
    if (doctorUserId != null)
      notifyUser(
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
            ? one("SELECT * FROM appointment WHERE id=? FOR UPDATE", appointmentId)
            : one(
                "SELECT * FROM appointment WHERE id=? AND patient_id=? FOR UPDATE",
                appointmentId,
                patientId);
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
              : jdbc.query(
                  "SELECT start_time FROM schedule_slot WHERE id=?",
                  rs -> rs.next() ? rs.getTime(1) : null,
                  ((Number) appointment.get("slot_id")).longValue());
      int cutoff = 30;
      try {
        Integer c =
                //读取系统配置中的取消截止时间
            jdbc.queryForObject(
                "SELECT CAST(config_value AS UNSIGNED) FROM system_config WHERE"
                    + " config_key='appointment.cancel.cutoff.minutes'",
                Integer.class);
        if (c != null) cutoff = c;
      } catch (Exception ignored) {
      }
      //如果时间段开始时间 < 当前时间 + 30分钟,不允取消
      if (slotTime instanceof java.sql.Time t
          && t.toLocalTime().isBefore(java.time.LocalTime.now().plusMinutes(cutoff)))
        throw new UserRegistrationException(409, "已超过取消预约截止时间");
    }
    //获取排班ID
    long scheduleId = ((Number) appointment.get("schedule_id")).longValue();
    lockSchedule(scheduleId);
    Object slotId = appointment.get("slot_id");
    if (slotId != null) lockSlot(((Number) slotId).longValue(), scheduleId);
    jdbc.update(
        "UPDATE appointment SET status=6,cancel_reason=?,cancelled_at=CURRENT_TIMESTAMP WHERE id=?",
        reason == null || reason.isBlank() ? "用户取消预约" : reason,
        appointmentId);
    if (slotId != null)
      //更新排班信息状态
      jdbc.update(
          "UPDATE schedule_slot SET status=0 WHERE id=? AND status=1",
          ((Number) slotId).longValue());
    jdbc.update(
        "UPDATE doctor_schedule SET booked_count=GREATEST(booked_count-1,0) WHERE id=?",
        scheduleId);
    jdbc.update(
        "UPDATE payment_record SET status=5,refunded_at=CURRENT_TIMESTAMP WHERE appointment_id=?"
            + " AND status=2",
        appointmentId);
    //写日志
    writeLog(operatorId, operation, "appointment", appointmentId, "取消预约并自动退款", ip);
    //写通知
    notifyUser(
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
            ? one("SELECT * FROM appointment WHERE id=? FOR UPDATE", appointmentId)
            : one(
                "SELECT * FROM appointment WHERE id=? AND patient_id=? FOR UPDATE",
                appointmentId,
                patientId);
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
    jdbc.update("UPDATE appointment SET status=3 WHERE id=?", appointmentId);
    //创建或复用就诊记录
    if (count("SELECT COUNT(*) FROM medical_visit WHERE appointment_id=?", appointmentId) == 0) {
      String visitNo =
          "V"
              + System.currentTimeMillis()
              + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
      jdbc.update(
          "INSERT INTO"
              + " medical_visit(appointment_id,patient_id,doctor_id,visit_no,status,check_in_at)"
              + " VALUES(?,?,?,?,1,CURRENT_TIMESTAMP)",
          appointmentId,
          appointment.get("patient_id"),
          appointment.get("doctor_id"),
          visitNo);
    }
    writeLog(operatorId, operation, "appointment", appointmentId, "预约签到", ip);
  }

  public List<Map<String, Object>> patientList(HttpServletRequest request) {
    AuthenticatedUser p = requirePatient(request);
    return jdbc.queryForList(
        "SELECT a.*,d.real_name doctor_name,dp.name department_name FROM appointment a JOIN doctor"
            + " d ON d.id=a.doctor_id JOIN department dp ON dp.id=a.department_id WHERE"
            + " a.patient_id=? ORDER BY a.appointment_date DESC,a.id DESC",
        p.patient_id());
  }

  public Map<String, Object> patientDetail(long id, HttpServletRequest request) {
    AuthenticatedUser p = requirePatient(request);
    return detail(id, p.patient_id(), null);
  }

  public List<Map<String, Object>> doctorList(HttpServletRequest request) {
    AuthenticatedUser d = requireDoctor(request);
    return jdbc.queryForList(
        "SELECT a.*,p.real_name patient_name,p.phone patient_phone FROM appointment a JOIN patient"
            + " p ON p.id=a.patient_id WHERE a.doctor_id=? ORDER BY"
            + " a.appointment_date,a.queue_no,a.id",
        d.doctor_id());
  }

  /**
   * 挂号员查询预约列表
   */
  public List<Map<String, Object>> registrationList(HttpServletRequest request) {
    requireRegistration(request);
    return jdbc.queryForList(
        "SELECT a.*,p.real_name patient_name,d.real_name doctor_name FROM appointment a JOIN"
            + " patient p ON p.id=a.patient_id JOIN doctor d ON d.id=a.doctor_id WHERE a.status IN"
            + " (1,2,3) ORDER BY a.appointment_date,a.queue_no,a.id");
  }

  /**
   * 挂号员查询预约队列
   */
  public List<Map<String, Object>> queue(HttpServletRequest request) {
    requireRegistration(request);
    return jdbc.queryForList(
        "SELECT a.*,p.real_name patient_name,d.real_name doctor_name FROM appointment a JOIN"
            + " patient p ON p.id=a.patient_id JOIN doctor d ON d.id=a.doctor_id WHERE a.status=3"
            + " ORDER BY a.queue_no,a.id");
  }

  public Map<String, Object> patientPage(
      HttpServletRequest request, int page, int size, Integer status) {
    AuthenticatedUser p = requirePatient(request);
    return pageQuery(
        "SELECT a.*,d.real_name doctor_name,dp.name department_name FROM appointment a JOIN doctor"
            + " d ON d.id=a.doctor_id JOIN department dp ON dp.id=a.department_id WHERE"
            + " a.patient_id=?"
            + (status == null ? "" : " AND a.status=?")
            + " ORDER BY a.appointment_date DESC,a.id DESC LIMIT ? OFFSET ?",
        "SELECT COUNT(*) FROM appointment WHERE patient_id=?"
            + (status == null ? "" : " AND status=?"),
        p.patient_id(),
        status,
        page,
        size);
  }

  public Map<String, Object> doctorPage(
      HttpServletRequest request, int page, int size, Integer status) {
    AuthenticatedUser d = requireDoctor(request);
    return pageQuery(
        "SELECT a.*,p.real_name patient_name,p.phone patient_phone FROM appointment a JOIN patient"
            + " p ON p.id=a.patient_id WHERE a.doctor_id=?"
            + (status == null ? "" : " AND a.status=?")
            + " ORDER BY a.appointment_date,a.queue_no,a.id LIMIT ? OFFSET ?",
        "SELECT COUNT(*) FROM appointment WHERE doctor_id=?"
            + (status == null ? "" : " AND status=?"),
        d.doctor_id(),
        status,
        page,
        size);
  }

  public Map<String, Object> registrationPage(
      HttpServletRequest request, int page, int size, Integer status) {
    requireRegistration(request);
    return pageQuery(
        "SELECT a.*,p.real_name patient_name,d.real_name doctor_name FROM appointment a JOIN"
            + " patient p ON p.id=a.patient_id JOIN doctor d ON d.id=a.doctor_id WHERE 1=1"
            + (status == null ? "" : " AND a.status=?")
            + " ORDER BY a.appointment_date,a.queue_no,a.id LIMIT ? OFFSET ?",
        "SELECT COUNT(*) FROM appointment WHERE 1=1" + (status == null ? "" : " AND status=?"),
        null,
        status,
        page,
        size);
  }

  /**
   * 用于返回更新后的预约信息
   * @param id
   * @param patientId
   * @param doctorId
   * @return
   */
  private Map<String, Object> detail(long id, Long patientId, Long doctorId) {
    String sql =
        "SELECT a.*,d.real_name doctor_name,dp.name department_name,p.real_name patient_name FROM"
            + " appointment a JOIN doctor d ON d.id=a.doctor_id JOIN department dp ON"
            + " dp.id=a.department_id JOIN patient p ON p.id=a.patient_id WHERE a.id=?";
    Map<String, Object> result = one(sql, id);
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
  private Map<String, Object> lockSchedule(long id) {
    return one("SELECT * FROM doctor_schedule WHERE id=? FOR UPDATE", id);
  }

  /**
   * 时间段校验
   * @param slotId
   * @param scheduleId
   * @return
   */
  private Map<String, Object> lockSlot(long slotId, long scheduleId) {
    Map<String, Object> s =
        one(
            "SELECT * FROM schedule_slot WHERE id=? AND schedule_id=? FOR UPDATE",
            slotId,
            scheduleId);
    if (s == null) throw new UserRegistrationException(404, "时间段不存在");
    return s;
  }

  /**
   * 查询当前排班最大的队列号然后+1
   * @param scheduleId
   * @return
   */
  private int nextQueueNo(long scheduleId) {
    Integer n =
        jdbc.queryForObject(
            "SELECT COALESCE(MAX(queue_no),0)+1 FROM appointment WHERE schedule_id=? FOR UPDATE",
            Integer.class,
            scheduleId);
    return n == null ? 1 : n;
  }

  /**
   * 插入预约记录,支付状态写2,完成支付
   * @param no 新生成预约号
   * @param patientId 患者ID
   * @param doctorId 病人ID
   * @param s 排班ID
   * @param r
   * @param q 排队号
   * @param fee 费用
   * @return
   */
  private long insertAppointment(
      String no,
      long patientId,
      long doctorId,
      Map<String, Object> s,
      CreateAppointmentRequest r,
      int q,
      BigDecimal fee) {
    KeyHolder holder = new GeneratedKeyHolder();
    int rows =
        jdbc.update(
            connection -> {
              PreparedStatement ps =
                  connection.prepareStatement(
                      "INSERT INTO"
                          + " appointment(appointment_no,patient_id,doctor_id,department_id,schedule_id,slot_id,appointment_date,period,queue_no,fee,status,remark)"
                          + " VALUES(?,?,?,?,?,?,?,?,?,?,2,?)",
                      Statement.RETURN_GENERATED_KEYS);
              ps.setString(1, no);
              ps.setLong(2, patientId);
              ps.setLong(3, doctorId);
              ps.setObject(4, s.get("department_id"));
              ps.setLong(5, r.schedule_id());
              ps.setObject(6, r.slot_id());
              ps.setObject(7, s.get("schedule_date"));
              ps.setObject(8, s.get("period"));
              ps.setInt(9, q);
              ps.setBigDecimal(10, fee);
              ps.setString(11, r.remark());
              return ps;
            },
            holder);
    if (rows != 1 || holder.getKey() == null) throw new IllegalStateException("创建预约失败");
    return holder.getKey().longValue();
  }

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
    jdbc.update(
        "INSERT INTO"
            + " payment_record(payment_no,appointment_id,patient_id,amount,payment_method,status,third_party_no,paid_at)"
            + " VALUES(?,?,?, ?,1,2,?,CURRENT_TIMESTAMP)",
        no,
        appointmentId,
        patientId,
        fee,
        "MOCK-" + no);
  }

  /**
   * 执行数据库语句
   * @param sql
   * @param args
   * @return
   */
  private Map<String, Object> one(String sql, Object... args) {
    return jdbc.query(sql, rs -> rs.next() ? row(rs) : null, args);
  }

  /**
   * 将数据库查询结果转换为Map<String, Object>
   * @param rs
   * @return
   * @throws java.sql.SQLException
   */
  private Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
    Map<String, Object> m = new java.util.LinkedHashMap<>();
    var md = rs.getMetaData();
    for (int i = 1; i <= md.getColumnCount(); i++) m.put(md.getColumnLabel(i), rs.getObject(i));
    return m;
  }

  /**
   * 执行计数语句
   * @param sql
   * @param args
   * @return
   */
  private long count(String sql, Object... args) {
    Long n = jdbc.queryForObject(sql, Long.class, args);
    return n == null ? 0 : n;
  }

  /**
   * 分页查询辅助方法
   * @param sql
   * @param countSql
   * @param owner
   * @param status
   * @param page
   * @param size
   * @return
   */
  private Map<String, Object> pageQuery(
      String sql, String countSql, Long owner, Integer status, int page, int size) {
    int pg = Math.max(1, page), s = Math.min(Math.max(1, size), 100), offset = (pg - 1) * s;
    List<Map<String, Object>> rows;
    Long total;
    if (owner == null) {
      if (status == null) {
        rows = jdbc.queryForList(sql, s, offset);
        total = jdbc.queryForObject(countSql, Long.class);
      } else {
        rows = jdbc.queryForList(sql, status, s, offset);
        total = jdbc.queryForObject(countSql, Long.class, status);
      }
    } else if (status == null) {
      rows = jdbc.queryForList(sql, owner, s, offset);
      total = jdbc.queryForObject(countSql, Long.class, owner);
    } else {
      rows = jdbc.queryForList(sql, owner, status, s, offset);
      total = jdbc.queryForObject(countSql, Long.class, owner, status);
    }
    Map<String, Object> m = new java.util.LinkedHashMap<>();
    m.put("page", pg);
    m.put("page_size", s);
    m.put("total", total);
    m.put("items", rows);
    return m;
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
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,?,?,?,?)",
        userId,
        type,
        target,
        id,
        desc,
        ip);
  }

  /**
   * 写入用户通知
   * @param userId 用户id
   * @param title 标题
   * @param content 内容
   * @param type 类型
   */
  private void notifyUser(long userId, String title, String content, String type) {
    jdbc.update(
        "INSERT INTO system_notification(user_id,title,content,notification_type) VALUES(?,?,?,?)",
        userId,
        title,
        content,
        type);
    String recipient =
        jdbc.query(
            "SELECT phone FROM patient WHERE user_id=? UNION ALL SELECT NULL FROM doctor WHERE"
                + " user_id=? LIMIT 1",
            rs -> rs.next() ? rs.getString(1) : null,
            userId,
            userId);
    if (recipient != null && !recipient.isBlank())
      jdbc.update(
          "INSERT INTO notification_outbox(user_id,channel,recipient,subject,content,status)"
              + " VALUES(?,?,?,?,?,0)",
          userId,
          "SMS",
          recipient,
          title,
          content);
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
}
