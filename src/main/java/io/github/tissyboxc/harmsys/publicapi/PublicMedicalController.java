package io.github.tissyboxc.harmsys.publicapi;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
/** 公开医生和科室介绍查询接口。 */
public class PublicMedicalController {
  private final JdbcTemplate jdbc;

  public PublicMedicalController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * 公开医生信息接口
   * @param department_id 科室ID
   * @param keyword 同时匹配医生真实姓名,编号,擅长领域（LIKE）
   */
  @GetMapping("/doctors")
  public List<Map<String, Object>> doctors(
      @RequestParam(required = false) Long department_id,
      @RequestParam(required = false) String keyword) {
    String sql =
        "SELECT"
            + " id,department_id,doctor_no,real_name,title,specialty,introduction,avatar_url,consultation_fee,status"
            + " FROM doctor WHERE deleted=0 AND status=1";
    List<Object> a = new ArrayList<>();
    if (department_id != null) {
      sql += " AND department_id=?";
      a.add(department_id);
    }
    if (keyword != null && !keyword.isBlank()) {
      sql += " AND (real_name LIKE ? OR doctor_no LIKE ? OR specialty LIKE ?)";
      String k = "%" + keyword.trim() + "%";
      a.add(k);
      a.add(k);
      a.add(k);
    }
    sql += " ORDER BY id";
    return jdbc.queryForList(sql, a.toArray());
  }

  /**
   * 根据医生ID查询医生信息
   * @param id 医生ID
   */
  @GetMapping("/doctors/{id}")
  public Map<String, Object> doctor(@PathVariable long id) {
    return jdbc.queryForMap(
        "SELECT"
            + " id,department_id,doctor_no,real_name,title,specialty,introduction,avatar_url,consultation_fee,status"
            + " FROM doctor WHERE id=? AND deleted=0 AND status=1",
        id);
  }

  /**
   * 查询指定科室的医生信息
   * @param id 科室ID
   */
  @GetMapping("/departments/{id}/doctors")
  public List<Map<String, Object>> departmentDoctors(@PathVariable long id) {
    return doctors(id, null);
  }

  /**
   * 查询所有排班信息
   * @param department_id 科室ID
   * @param doctor_id 医生ID
   * @param schedule_date 排班日期
   * @param period 时间段
   */
  @GetMapping("/schedules")
  public List<Map<String, Object>> schedules(
      @RequestParam(required = false) Long department_id,
      @RequestParam(required = false) Long doctor_id,
      @RequestParam(required = false) String schedule_date,
      @RequestParam(required = false) Integer period) {
    String sql =
        "SELECT s.id,s.doctor_id,s.department_id,d.real_name"
            + " doctor_name,s.schedule_date,s.period,s.start_time,s.end_time,s.total_count,s.booked_count,(s.total_count-s.booked_count)"
            + " remaining_count,s.fee,s.status,s.remark FROM doctor_schedule s JOIN doctor d ON"
            + " d.id=s.doctor_id WHERE s.status=1 AND d.status=1 AND d.deleted=0 AND"
            + " s.schedule_date>=CURRENT_DATE";
    List<Object> a = new ArrayList<>();
    if (department_id != null) {
      sql += " AND s.department_id=?";
      a.add(department_id);
    }
    if (doctor_id != null) {
      sql += " AND s.doctor_id=?";
      a.add(doctor_id);
    }
    if (schedule_date != null) {
      sql += " AND s.schedule_date=?";
      a.add(schedule_date);
    }
    if (period != null) {
      sql += " AND s.period=?";
      a.add(period);
    }
    sql += " ORDER BY s.schedule_date,s.start_time";
    return jdbc.queryForList(sql, a.toArray());
  }

  /**
   * 根据排班ID查询
   * @param id 排班ID
   */
  @GetMapping("/schedules/{id}")
  public Map<String, Object> schedule(@PathVariable long id) {
    return jdbc.queryForMap("SELECT * FROM doctor_schedule WHERE id=? AND status=1", id);
  }

  /**
   * 查询指定排班的时间段
   * @param id 排班ID
   * @return 该排班的时间段信息
   */
  @GetMapping("/schedules/{id}/slots")
  public List<Map<String, Object>> slots(@PathVariable long id) {
    return jdbc.queryForList(
        "SELECT id,schedule_id,slot_no,start_time,end_time,status FROM schedule_slot WHERE"
            + " schedule_id=? AND status=0 ORDER BY slot_no",
        id);
  }
}
