package io.github.tissyboxc.harmsys.doctor;

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

@RestController
/** 排班停诊、批量取消预约和状态生命周期管理。 */
public class ScheduleLifecycleController {
  private final JdbcTemplate jdbc;

  public ScheduleLifecycleController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * 管理员取消指定排班->停诊
   * @param id 排班ID
   * @param body 包含原因的请求体
   */
  @PostMapping("/api/admin/schedules/{id}/stop")
  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> adminStop(
      @PathVariable long id,
      @RequestBody(required = false) Map<String, Object> body,
      HttpServletRequest request) {
    AuthenticatedUser op = admin(request);
    return stop(
        id,
        body == null ? "管理员停诊" : String.valueOf(body.getOrDefault("reason", "管理员停诊")),
        op.user_id(),
        request);
  }

  /**
   * 医生停诊
   * @param id 医生ID
   * @param body 包含原因的请求体
   */
  @PostMapping("/api/doctor/schedules/{id}/stop")
  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> doctorStop(
      @PathVariable long id,
      @RequestBody(required = false) Map<String, Object> body,
      HttpServletRequest request) {
    AuthenticatedUser op = doctor(request);
    Map<String, Object> s =
        one(
            "SELECT * FROM doctor_schedule WHERE id=? AND doctor_id=? FOR UPDATE",
            id,
            op.doctor_id());
    if (s == null) throw new UserRegistrationException(404, "排班不存在或不属于当前医生");
    return stopLocked(
        id,
        body == null ? "医生停诊" : String.valueOf(body.getOrDefault("reason", "医生停诊")),
        op.user_id(),
        request);
  }

  /**
   * 管理员修改排班状态
   * @param id 排班ID
   * @param body 包含状态码的请求体
   */
  @PutMapping("/api/admin/schedules/{id}/status")
  public Map<String, Object> status(
      @PathVariable long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
    AuthenticatedUser op = admin(request);
    int status = Integer.parseInt(String.valueOf(body.get("status")));
    if (status < 0 || status > 3) throw new UserRegistrationException(422, "排班状态只能为0到3");
    if (jdbc.update("UPDATE doctor_schedule SET status=? WHERE id=?", status, id) != 1)
      throw new UserRegistrationException(404, "排班不存在");
    log(op.user_id(), "ADMIN_UPDATE_SCHEDULE_STATUS", "doctor_schedule", id, "管理员修改排班状态", request);
    return one("SELECT * FROM doctor_schedule WHERE id=?", id);
  }

  /**
   * 管理员修改时间段信息
   * @param slotId 时间段ID
   * @param body 修改内容
   */
  @PutMapping("/api/admin/schedules/slots/{slotId}")
  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> updateAdminSlot(
      @PathVariable long slotId,
      @RequestBody Map<String, Object> body,
      HttpServletRequest request) {
    AuthenticatedUser op = admin(request);
    Map<String, Object> slot = one("SELECT * FROM schedule_slot WHERE id=?", slotId);
    if (slot == null) throw new UserRegistrationException(404, "时间段不存在");
    int status =
        body.get("status") == null
            ? ((Number) slot.get("status")).intValue()
            : Integer.parseInt(String.valueOf(body.get("status")));
    if (status < 0 || status > 2) throw new UserRegistrationException(422, "时间段状态只能为0、1、2");
    if (status != 0
        && jdbc.queryForObject(
                "SELECT COUNT(*) FROM appointment WHERE slot_id=? AND status IN (1,2,3,4)",
                Long.class,
                slotId)
            > 0) throw new UserRegistrationException(409, "已有预约的时间段不能锁定或修改");
    jdbc.update("UPDATE schedule_slot SET status=? WHERE id=?", status, slotId);
    log(op.user_id(), "ADMIN_UPDATE_SLOT", "schedule_slot", slotId, "管理员修改时间段状态", request);
    return one("SELECT * FROM schedule_slot WHERE id=?", slotId);
  }

  /**
   * 管理员删除指定时间段
   * @param slotId 时间段ID
   */
  @DeleteMapping("/api/admin/schedules/slots/{slotId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional(rollbackFor = Exception.class)
  public void deleteAdminSlot(@PathVariable long slotId, HttpServletRequest request) {
    AuthenticatedUser op = admin(request);
    Map<String, Object> slot = one("SELECT * FROM schedule_slot WHERE id=?", slotId);
    if (slot == null) throw new UserRegistrationException(404, "时间段不存在");
    if (((Number) slot.get("status")).intValue() != 0
        || jdbc.queryForObject(
                "SELECT COUNT(*) FROM appointment WHERE slot_id=? AND status IN (1,2,3,4)",
                Long.class,
                slotId)
            > 0) throw new UserRegistrationException(409, "已有预约的时间段不能删除");
    jdbc.update("DELETE FROM schedule_slot WHERE id=?", slotId);
    log(op.user_id(), "ADMIN_DELETE_SLOT", "schedule_slot", slotId, "管理员删除时间段", request);
  }

  /**
   * 停诊处理
   * @param id 排班ID
   * @param reason 原因
   * @param uid 操作者ID
   */
  private Map<String, Object> stop(long id, String reason, long uid, HttpServletRequest request) {
    Map<String, Object> s = one("SELECT * FROM doctor_schedule WHERE id=? FOR UPDATE", id);
    if (s == null) throw new UserRegistrationException(404, "排班不存在");
    return stopLocked(id, reason, uid, request);
  }

  /**
   *医生排班停诊,并处理已预约信息
   */
  private Map<String, Object> stopLocked(
      long id, String reason, long uid, HttpServletRequest request) {
    jdbc.update("UPDATE doctor_schedule SET status=2,remark=? WHERE id=?", reason, id);
    List<Map<String, Object>> list =
        jdbc.queryForList(
            "SELECT id,slot_id FROM appointment WHERE schedule_id=? AND status IN (1,2,3,4)", id);
    for (Map<String, Object> a : list) {
      long aid = ((Number) a.get("id")).longValue();
      //更新预约信息
      jdbc.update(
          "UPDATE appointment SET status=7,cancel_reason=?,cancelled_at=CURRENT_TIMESTAMP WHERE"
              + " id=?",
          reason,
          aid);
      //释放号源
      if (a.get("slot_id") != null)
        jdbc.update(
            "UPDATE schedule_slot SET status=0 WHERE id=? AND status=1",
            ((Number) a.get("slot_id")).longValue());
      //退款处理
      jdbc.update(
          "UPDATE payment_record SET status=5,refunded_at=CURRENT_TIMESTAMP WHERE appointment_id=?"
              + " AND status=2",
          aid);
    }
    //更新已预约数为0
    jdbc.update("UPDATE doctor_schedule SET booked_count=0 WHERE id=?", id);
    log(uid, "STOP_SCHEDULE", "doctor_schedule", id, "停诊并批量取消预约", request);
    return one("SELECT * FROM doctor_schedule WHERE id=?", id);
  }

  /**
   * 验证已登录同时验证管理员身份
   */
  private AuthenticatedUser admin(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "只有管理员可以停诊");
    return u;
  }

  /**
   * 验证已登录同时验证医生身份
   */
  private AuthenticatedUser doctor(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.doctor_id() == null
        || u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("DOCTOR")))
      throw new SessionAuthenticationException(403, "当前账号不是医生");
    return u;
  }

  private Map<String, Object> one(String sql, Object... a) {
    return jdbc.query(sql, rs -> rs.next() ? row(rs) : null, a);
  }

  private Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
    Map<String, Object> m = new LinkedHashMap<>();
    var md = rs.getMetaData();
    for (int i = 1; i <= md.getColumnCount(); i++) m.put(md.getColumnLabel(i), rs.getObject(i));
    return m;
  }

  /**
   * 统一日志逻辑
   */
  private void log(
      long uid, String type, String target, long id, String desc, HttpServletRequest r) {
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,?,?,?,?)",
        uid,
        type,
        target,
        id,
        desc,
        r.getRemoteAddr());
  }
}
