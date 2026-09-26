package io.github.tissyboxc.harmsys.publicapi.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 公开医生、科室与排班查询的数据访问层。 */
@Repository
public class PublicMedicalRepository {
  private final JdbcTemplate jdbc;

  public PublicMedicalRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> doctors(Long departmentId, String keyword) {
    String sql =
        "SELECT"
            + " d.id,d.department_id,dp.name department_name,d.doctor_no,d.real_name,d.title,"
            + "d.specialty,d.introduction,d.avatar_url,d.consultation_fee,d.status"
            + " FROM doctor d JOIN department dp ON dp.id=d.department_id"
            + " WHERE d.deleted=0 AND d.status=1";
    List<Object> args = new ArrayList<>();
    if (departmentId != null) {
      sql += " AND d.department_id=?";
      args.add(departmentId);
    }
    if (keyword != null && !keyword.isBlank()) {
      sql += " AND (d.real_name LIKE ? OR d.doctor_no LIKE ? OR d.specialty LIKE ?)";
      String like = "%" + keyword.trim() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
    }
    return jdbc.queryForList(sql + " ORDER BY d.id", args.toArray());
  }

  public Map<String, Object> doctor(long id) {
    return jdbc.queryForMap(
        "SELECT"
            + " d.id,d.department_id,dp.name department_name,d.doctor_no,d.real_name,d.title,"
            + "d.specialty,d.introduction,d.avatar_url,d.consultation_fee,d.status"
            + " FROM doctor d JOIN department dp ON dp.id=d.department_id"
            + " WHERE d.id=? AND d.deleted=0 AND d.status=1",
        id);
  }

  public List<Map<String, Object>> schedules(
      Long departmentId, Long doctorId, String scheduleDate, Integer period) {
    String sql =
        "SELECT s.id,s.doctor_id,s.department_id,d.real_name"
            + " doctor_name,dp.name department_name,s.schedule_date,s.period,s.start_time,s.end_time,"
            + "s.total_count,s.booked_count,(s.total_count-s.booked_count) remaining_count,s.fee,s.status,"
            + "s.remark FROM doctor_schedule s JOIN doctor d ON d.id=s.doctor_id JOIN department dp ON"
            + " dp.id=s.department_id WHERE s.status=1 AND d.status=1 AND d.deleted=0 AND"
            + " s.schedule_date>=CURRENT_DATE";
    List<Object> args = new ArrayList<>();
    if (departmentId != null) {
      sql += " AND s.department_id=?";
      args.add(departmentId);
    }
    if (doctorId != null) {
      sql += " AND s.doctor_id=?";
      args.add(doctorId);
    }
    if (scheduleDate != null) {
      sql += " AND s.schedule_date=?";
      args.add(scheduleDate);
    }
    if (period != null) {
      sql += " AND s.period=?";
      args.add(period);
    }
    return jdbc.queryForList(sql + " ORDER BY s.schedule_date,s.start_time", args.toArray());
  }

  public Map<String, Object> schedule(long id) {
    return jdbc.queryForMap("SELECT * FROM doctor_schedule WHERE id=? AND status=1", id);
  }

  public List<Map<String, Object>> slots(long scheduleId) {
    return jdbc.queryForList(
        "SELECT id,schedule_id,slot_no,start_time,end_time,status FROM schedule_slot WHERE"
            + " schedule_id=? AND status=0 ORDER BY slot_no",
        scheduleId);
  }
}
