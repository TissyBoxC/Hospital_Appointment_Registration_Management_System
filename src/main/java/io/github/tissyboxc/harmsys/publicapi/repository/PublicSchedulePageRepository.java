package io.github.tissyboxc.harmsys.publicapi.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 公开可预约排班分页查询的数据访问层。 */
@Repository
public class PublicSchedulePageRepository {
  private final JdbcTemplate jdbc;

  public PublicSchedulePageRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> page(
      Long departmentId, Long doctorId, int size, int offset) {
    Query query = query(departmentId, doctorId);
    List<Object> args = new ArrayList<>(query.args());
    args.add(size);
    args.add(offset);
    return jdbc.queryForList(query.sql() + " ORDER BY ds.schedule_date,ds.start_time LIMIT ? OFFSET ?", args.toArray());
  }

  public long total(Long departmentId, Long doctorId) {
    Query query = query(departmentId, doctorId);
    Long total =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM doctor_schedule ds JOIN doctor d ON d.id=ds.doctor_id"
                + query.where(),
            Long.class,
            query.args().toArray());
    return total == null ? 0 : total;
  }

  private Query query(Long departmentId, Long doctorId) {
    StringBuilder where =
        new StringBuilder(
            " WHERE ds.status=1 AND ds.schedule_date>=CURRENT_DATE AND"
                + " ds.booked_count<ds.total_count AND d.status=1 AND d.deleted=0");
    List<Object> args = new ArrayList<>();
    if (departmentId != null) {
      where.append(" AND ds.department_id=?");
      args.add(departmentId);
    }
    if (doctorId != null) {
      where.append(" AND ds.doctor_id=?");
      args.add(doctorId);
    }
    String sql =
        "SELECT ds.id,ds.doctor_id,ds.department_id,d.real_name"
            + " doctor_name,dp.name department_name,ds.schedule_date,ds.period,ds.start_time,ds.end_time,"
            + "ds.total_count,ds.booked_count,(ds.total_count-ds.booked_count) remaining_count,ds.fee,"
            + "ds.status,ds.remark FROM doctor_schedule ds JOIN doctor d ON d.id=ds.doctor_id JOIN"
            + " department dp ON dp.id=ds.department_id";
    return new Query(sql, where.toString(), args);
  }

  private record Query(String sql, String where, List<Object> args) {}
}
