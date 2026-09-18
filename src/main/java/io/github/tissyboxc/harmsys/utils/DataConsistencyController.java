package io.github.tissyboxc.harmsys.utils;

import io.github.tissyboxc.harmsys.users.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** 管理员手工执行数据一致性检查；定时任务也会自动执行同类修复。 */
@RestController
@RequestMapping("/api/admin/data-consistency")
/** 管理员检查和修复业务数据一致性的接口。 */
public class DataConsistencyController {
  private final JdbcTemplate jdbc;

  public DataConsistencyController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * 查询,统计
   * @return “scedule_booked_count_mismatch+orphan_reserved_slots+payment_status_mismatch+orphan_slots”
   */
  @GetMapping
  public Map<String, Object> check(HttpServletRequest request) {
    admin(request);
    Map<String, Object> result = new LinkedHashMap<>();
    //排班已预约数量不一致,检查doctor_schedule.booked_count和该排班下的有效预约数量
    result.put(
        "schedule_booked_count_mismatch",
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM doctor_schedule ds LEFT JOIN (SELECT schedule_id,COUNT(*) cnt"
                + " FROM appointment WHERE status IN (1,2,3,4) GROUP BY schedule_id) x ON"
                + " x.schedule_id=ds.id WHERE ds.booked_count<>COALESCE(x.cnt,0)",
            Long.class));
    //孤立占用时间段
    result.put(
        "orphan_reserved_slots",
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM schedule_slot s LEFT JOIN appointment a ON a.slot_id=s.id AND"
                + " a.status IN (1,2,3,4) WHERE s.status=1 AND a.id IS NULL",
            Long.class));
    //支付状态与预约状态不一致,支付记录显示成功,但是预约已取消,过期,退款,停诊等
    result.put(
        "payment_status_mismatch",
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM payment_record p JOIN appointment a ON a.id=p.appointment_id"
                + " WHERE p.status=2 AND a.status IN (6,7,8,9)",
            Long.class));
    //孤立号源时间段-(约束大部分情况不会被绕过,我不会人工导入数据,也不会迁移留下脏数据)按理说不会出现,repair也就没写
    result.put(
        "orphan_slots",
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM schedule_slot s LEFT JOIN doctor_schedule ds ON"
                + " ds.id=s.schedule_id WHERE ds.id IS NULL",
            Long.class));
    return result;
  }

  /**
   * 修复接口
   */
  @PostMapping("/repair")
  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> repair(HttpServletRequest request) {
    AuthenticatedUser user = admin(request);
    int counts = 0;
    //矫正booked_count
    counts +=
        jdbc.update(
            "UPDATE doctor_schedule ds LEFT JOIN (SELECT schedule_id,COUNT(*) cnt FROM appointment"
                + " WHERE status IN (1,2,3,4) GROUP BY schedule_id) x ON x.schedule_id=ds.id SET"
                + " ds.booked_count=COALESCE(x.cnt,0) WHERE ds.booked_count<>COALESCE(x.cnt,0)");
    //释放孤立已占用时间段
    counts +=
        jdbc.update(
            "UPDATE schedule_slot s LEFT JOIN appointment a ON a.slot_id=s.id AND a.status IN"
                + " (1,2,3,4) SET s.status=0 WHERE s.status=1 AND a.id IS NULL");
    //修正支付状态
    counts +=
        jdbc.update(
            "UPDATE payment_record p JOIN appointment a ON a.id=p.appointment_id SET"
                + " p.status=5,p.refunded_at=COALESCE(p.refunded_at,CURRENT_TIMESTAMP),p.refund_reason=COALESCE(p.refund_reason,'管理员一致性修复')"
                + " WHERE p.status=2 AND a.status IN (6,7,8,9)");
    jdbc.update(
        "INSERT INTO operation_log(user_id,operation_type,target_type,description) VALUES(?,?,?,?)",
        user.user_id(),
        "REPAIR_DATA_CONSISTENCY",
        "system",
        "管理员执行数据一致性修复，共影响" + counts + "条记录");
    Map<String, Object> result = check(request);
    result.put("affected_rows", counts);
    return result;
  }

  /**
   * 管理员身份校验
   */
  private AuthenticatedUser admin(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "只有管理员可以执行数据一致性检查");
    return u;
  }
}
