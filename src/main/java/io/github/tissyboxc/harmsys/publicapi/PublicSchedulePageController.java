package io.github.tissyboxc.harmsys.publicapi;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/** 公共排班分页查询，返回有剩余号源的可预约排班。 */
@RestController
@RequestMapping("/api/public/schedules")
/** 公开排班分页查询接口。 */
public class PublicSchedulePageController {
  private final JdbcTemplate jdbc;

  public PublicSchedulePageController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/page")
  public Map<String, Object> page(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int page_size,
      @RequestParam(required = false) Long department_id,
      @RequestParam(required = false) Long doctor_id) {
    int pg = Math.max(1, page), s = Math.min(Math.max(1, page_size), 100), off = (pg - 1) * s;
    StringBuilder w =
        new StringBuilder(
            " WHERE ds.status=1 AND ds.schedule_date>=CURRENT_DATE AND"
                + " ds.booked_count<ds.total_count AND d.status=1 AND d.deleted=0");
    List<Object> a = new ArrayList<>();
    if (department_id != null) {
      w.append(" AND ds.department_id=?");
      a.add(department_id);
    }
    if (doctor_id != null) {
      w.append(" AND ds.doctor_id=?");
      a.add(doctor_id);
    }
    String sql =
        "SELECT ds.id,ds.doctor_id,ds.department_id,d.real_name"
            + " doctor_name,ds.schedule_date,ds.period,ds.start_time,ds.end_time,ds.total_count,ds.booked_count,(ds.total_count-ds.booked_count)"
            + " remaining_count,ds.fee,ds.status,ds.remark FROM doctor_schedule ds JOIN doctor d ON"
            + " d.id=ds.doctor_id"
            + w
            + " ORDER BY ds.schedule_date,ds.start_time LIMIT ? OFFSET ?";
    List<Object> qa = new ArrayList<>(a);
    qa.add(s);
    qa.add(off);
    List<Map<String, Object>> items = jdbc.queryForList(sql, qa.toArray());
    long total =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM doctor_schedule ds JOIN doctor d ON d.id=ds.doctor_id" + w,
            Long.class,
            a.toArray());
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("page", pg);
    m.put("page_size", s);
    m.put("total", total);
    m.put("items", items);
    return m;
  }
}
