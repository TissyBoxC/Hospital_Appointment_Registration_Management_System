package io.github.tissyboxc.harmsys.appointment;

import io.github.tissyboxc.harmsys.users.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.users.UserRegistrationException;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员对预约执行操作
 */
@RestController
@RequestMapping("/api/admin/appointments")
public class AdminAppointmentOperationsController {
  private final JdbcTemplate jdbc;

  public AdminAppointmentOperationsController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   *
   * @param id 存放用户id
   * @param body 存放修改内容
   * @return
   */
  @PutMapping("/{id}")
  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> update(
      @PathVariable long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    AuthenticatedUser operator = admin(request);
    //锁定预约记录,预防并发修改
    Map<String, Object> a = one("SELECT * FROM appointment WHERE id=? FOR UPDATE", id);
    if (a == null) throw new UserRegistrationException(404, "预约不存在");
    //如果请求体存在schedule_id或slot_id则记为需要迁移
    if (body.containsKey("schedule_id") || body.containsKey("slot_id")) {
      migrate(id, a, body, operator, request);
      a = one("SELECT * FROM appointment WHERE id=? FOR UPDATE", id);
    }
    String remark =
        body.get("remark") == null ? (String) a.get("remark") : String.valueOf(body.get("remark"));
    Integer status =
        body.get("status") == null
            ? ((Number) a.get("status")).intValue()
            : Integer.valueOf(String.valueOf(body.get("status")));
    Integer queueNo =
        body.get("queue_no") == null
            ? (Integer) a.get("queue_no")
            : Integer.valueOf(String.valueOf(body.get("queue_no")));
    if (status < 1 || status > 9) throw new UserRegistrationException(422, "预约状态必须为1到9");
    //预约退款
    int oldStatus = ((Number) a.get("status")).intValue();
    //从有效变无效->释放当前排班
    if (oldStatus >= 1 && oldStatus <= 4 && !(status >= 1 && status <= 4)) {
      release(a);
      if (status == 6 || status == 7 || status == 8 || status == 9)
        jdbc.update(
            "UPDATE payment_record SET"
                + " status=5,refunded_at=COALESCE(refunded_at,CURRENT_TIMESTAMP),refund_reason=COALESCE(refund_reason,'管理员修改预约状态')"
                + " WHERE appointment_id=? AND status=2",
            id);
    }
    //从无效变有效->尝试回复号源
    else if (!(oldStatus >= 1 && oldStatus <= 4) && status >= 1 && status <= 4) {
      Map<String, Object> schedule =
          one(
              "SELECT * FROM doctor_schedule WHERE id=? FOR UPDATE",
              ((Number) a.get("schedule_id")).longValue());
      if (schedule == null
          || ((Number) schedule.get("status")).intValue() == 2
          || ((Number) schedule.get("status")).intValue() == 3
          || ((Number) schedule.get("booked_count")).intValue()
              >= ((Number) schedule.get("total_count")).intValue())
        throw new UserRegistrationException(409, "无法恢复预约，排班号源不可用");
      //有效变有效或无效变无效
      if (a.get("slot_id") != null
          && jdbc.update(
                  "UPDATE schedule_slot SET status=1 WHERE id=? AND status=0",
                  ((Number) a.get("slot_id")).longValue())
              != 1) throw new UserRegistrationException(409, "无法恢复预约，时间段不可用");
      jdbc.update(
          "UPDATE doctor_schedule SET booked_count=booked_count+1 WHERE id=?",
          ((Number) a.get("schedule_id")).longValue());
    }
    //返回更新后的预约信息
    jdbc.update(
        "UPDATE appointment SET queue_no=?,status=?,remark=? WHERE id=?",
        queueNo,
        status,
        remark,
        id);
    log(operator.user_id(), "ADMIN_UPDATE_APPOINTMENT", id, "管理员修改预约", request);
    return one(
        "SELECT a.*,p.real_name patient_name,d.real_name doctor_name,dp.name department_name FROM"
            + " appointment a JOIN patient p ON p.id=a.patient_id JOIN doctor d ON d.id=a.doctor_id"
            + " JOIN department dp ON dp.id=a.department_id WHERE a.id=?",
        id);
  }

  /**
   *update的辅助私有方法,完成换排班/时间段的迁移,同步释放旧号源占用新号源
   * @param id 用户id
   * @param current 迁移前拿到的预约记录
   * @param body 请求体中的新预约记录
   */
  private void migrate(
      long id,
      Map<String, Object> current,
      Map<String, Object> body,
      AuthenticatedUser operator,
      HttpServletRequest request) {
    //校验当前预约状态,1-4才可以迁移
    int currentStatus = ((Number) current.get("status")).intValue();
    if (currentStatus < 1 || currentStatus > 4)
      throw new UserRegistrationException(409, "只有有效预约可以迁移");
    //确定目标排班
    long targetSchedule =
        body.get("schedule_id") == null
            ? ((Number) current.get("schedule_id")).longValue()
            : Long.parseLong(String.valueOf(body.get("schedule_id")));
    //确定目标时间段
    Long targetSlot =
        body.containsKey("slot_id") && body.get("slot_id") != null
            ? Long.parseLong(String.valueOf(body.get("slot_id")))
            : (body.containsKey("slot_id")
                ? null
                //没有传入slot_id,但原slot_id不适用
                : (targetSchedule == ((Number) current.get("schedule_id")).longValue()
                        && current.get("slot_id") != null
                    ? ((Number) current.get("slot_id")).longValue()
                    : null));
    //锁定,校验目前排班
    Map<String, Object> schedule =
        one("SELECT * FROM doctor_schedule WHERE id=? FOR UPDATE", targetSchedule);
    if (schedule == null
        || ((Number) schedule.get("status")).intValue() == 2
        || ((Number) schedule.get("status")).intValue() == 3)
      throw new UserRegistrationException(409, "目标排班不存在或不可预约");
    if (schedule.get("schedule_date") instanceof java.sql.Date d
        && d.toLocalDate().isBefore(java.time.LocalDate.now()))
      throw new UserRegistrationException(409, "目标排班日期已过");
    //记录旧排班和时间段
    long oldSchedule = ((Number) current.get("schedule_id")).longValue();
    Long oldSlot =
        current.get("slot_id") == null ? null : ((Number) current.get("slot_id")).longValue();
    //防止重复预约同一排班
    if (targetSchedule != oldSchedule
        && count(
                "SELECT COUNT(*) FROM appointment WHERE patient_id=? AND schedule_id=? AND id<>?"
                    + " AND status IN (1,2,3,4)",
                current.get("patient_id"),
                targetSchedule,
                id)
            > 0) throw new UserRegistrationException(409, "患者已预约目标排班");
    if (targetSchedule != oldSchedule
            //检查目标排班号源是否已满
        && ((Number) schedule.get("booked_count")).intValue()
            >= ((Number) schedule.get("total_count")).intValue())
      throw new UserRegistrationException(409, "目标排班号源已满");
    if (targetSlot != null) {
      Map<String, Object> slot =
          one(
              "SELECT * FROM schedule_slot WHERE id=? AND schedule_id=? FOR UPDATE",
              targetSlot,
              targetSchedule);
      if (slot == null || ((Number) slot.get("status")).intValue() != 0)
        throw new UserRegistrationException(409, "目标时间段不可用");
    }
    //释放旧时间段,占用新时间段
    if (oldSlot != null && !Objects.equals(oldSlot, targetSlot))
      jdbc.update("UPDATE schedule_slot SET status=0 WHERE id=? AND status=1", oldSlot);
    if (targetSlot != null && !Objects.equals(oldSlot, targetSlot))
      jdbc.update("UPDATE schedule_slot SET status=1 WHERE id=? AND status=0", targetSlot);
    if (targetSchedule != oldSchedule) {
      jdbc.update(
          "UPDATE doctor_schedule SET booked_count=GREATEST(booked_count-1,0) WHERE id=?",
          oldSchedule);
      if (jdbc.update(
              "UPDATE doctor_schedule SET booked_count=booked_count+1 WHERE id=? AND"
                  + " booked_count<total_count",
              targetSchedule)
          != 1) throw new UserRegistrationException(409, "目标排班号源已满");
    }
    //更新排班信息
    jdbc.update(
        "UPDATE appointment SET"
            + " doctor_id=?,department_id=?,schedule_id=?,slot_id=?,appointment_date=?,period=?,fee=?"
            + " WHERE id=?",
        schedule.get("doctor_id"),
        schedule.get("department_id"),
        targetSchedule,
        targetSlot,
        schedule.get("schedule_date"),
        schedule.get("period"),
        schedule.get("fee"),
        id);
    log(
        operator.user_id(),
        "ADMIN_MIGRATE_APPOINTMENT",
        id,
        "管理员迁移预约到排班" + targetSchedule,
        request);
  }

  /**
   * 将有效预约置为“7”,释放号源并退款
   * @param id 用户id
   * @param body 可空请求体
   */
  @PostMapping("/{id}/cancel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional(rollbackFor = Exception.class)
  public void cancel(
      @PathVariable long id,
      @RequestBody(required = false) Map<String, Object> body,
      HttpServletRequest request) {
    AuthenticatedUser operator = admin(request);
    //查询当前用户的预约
    Map<String, Object> a = one("SELECT * FROM appointment WHERE id=? FOR UPDATE", id);
    if (a == null) throw new UserRegistrationException(404, "预约不存在");
    int status = ((Number) a.get("status")).intValue();
    //已就诊/完成的预约不能取消
    if (status < 1 || status > 4) throw new UserRegistrationException(409, "当前预约不能取消");
    String reason =
        body == null || body.get("reason") == null ? "管理员取消预约" : String.valueOf(body.get("reason"));
    jdbc.update(
        "UPDATE appointment SET status=7,cancel_reason=?,cancelled_at=CURRENT_TIMESTAMP WHERE id=?",
        reason,
        id);
    release(a);
    jdbc.update(
        "UPDATE payment_record SET status=5,refunded_at=CURRENT_TIMESTAMP WHERE appointment_id=?"
            + " AND status=2",
        id);
    log(operator.user_id(), "ADMIN_CANCEL_APPOINTMENT", id, reason, request);
  }

  /**
   * 将待支付/已过期的预约置"8"已过期
   * @param id 用户id
   */
  @PostMapping("/{id}/expire")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional(rollbackFor = Exception.class)
  public void expire(@PathVariable long id, HttpServletRequest request) {
    AuthenticatedUser operator = admin(request);
    Map<String, Object> a = one("SELECT * FROM appointment WHERE id=? FOR UPDATE", id);
    if (a == null) throw new UserRegistrationException(404, "预约不存在");
    if (jdbc.update(
            "UPDATE appointment SET status=8,cancel_reason='管理员标记过期' WHERE id=? AND status IN"
                + " (1,2)",
            id)
        != 1) throw new UserRegistrationException(409, "预约当前不能标记过期");
    release(a);
    log(operator.user_id(), "ADMIN_EXPIRE_APPOINTMENT", id, "管理员标记预约过期", request);
  }

  /**
   * 释放时间段并减少预约数
   */
  private void release(Map<String, Object> a) {
    Object slot = a.get("slot_id");
    if (slot != null)
      jdbc.update(
          "UPDATE schedule_slot SET status=0 WHERE id=? AND status=1", ((Number) slot).longValue());
    jdbc.update(
        "UPDATE doctor_schedule SET booked_count=GREATEST(booked_count-1,0) WHERE id=?",
        ((Number) a.get("schedule_id")).longValue());
  }


  /**
   * 执行sql语句,返回json体
   */
  private Map<String, Object> one(String sql, Object... args) {
    return jdbc.query(sql, rs -> rs.next() ? row(rs) : null, args);
  }

  private long count(String sql, Object... args) {
    Long n = jdbc.queryForObject(sql, Long.class, args);
    return n == null ? 0 : n;
  }

  private Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
    Map<String, Object> m = new LinkedHashMap<>();
    var md = rs.getMetaData();
    for (int i = 1; i <= md.getColumnCount(); i++) m.put(md.getColumnLabel(i), rs.getObject(i));
    return m;
  }


  /**
   * 验证管理员身份
   */
  private AuthenticatedUser admin(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "只有管理员可以执行该操作");
    return u;
  }


  /**
   * 向数据库写日志
   */
  private void log(long uid, String type, long id, String desc, HttpServletRequest r) {
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,?, ?,?,?)",
        uid,
        type,
        "appointment",
        id,
        desc,
        r.getRemoteAddr());
  }
}
