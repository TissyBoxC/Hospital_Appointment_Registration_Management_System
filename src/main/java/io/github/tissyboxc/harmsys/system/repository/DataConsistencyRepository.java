package io.github.tissyboxc.harmsys.system.repository;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 业务数据一致性检查和修复的数据访问层。 */
@Repository
public class DataConsistencyRepository {
  private final JdbcTemplate jdbc;

  public DataConsistencyRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Map<String, Object> check() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put(
        "schedule_booked_count_mismatch",
        count(
            "SELECT COUNT(*) FROM doctor_schedule ds LEFT JOIN (SELECT schedule_id,COUNT(*) cnt"
                + " FROM appointment WHERE status IN (1,2,3,4) GROUP BY schedule_id) x ON"
                + " x.schedule_id=ds.id WHERE ds.booked_count<>COALESCE(x.cnt,0)"));
    result.put(
        "orphan_reserved_slots",
        count(
            "SELECT COUNT(*) FROM schedule_slot s LEFT JOIN appointment a ON a.slot_id=s.id AND"
                + " a.status IN (1,2,3,4) WHERE s.status=1 AND a.id IS NULL"));
    result.put(
        "payment_status_mismatch",
        count(
            "SELECT COUNT(*) FROM payment_record p JOIN appointment a ON a.id=p.appointment_id"
                + " WHERE p.status=2 AND a.status IN (6,7,8,9)"));
    result.put(
        "orphan_slots",
        count(
            "SELECT COUNT(*) FROM schedule_slot s LEFT JOIN doctor_schedule ds ON"
                + " ds.id=s.schedule_id WHERE ds.id IS NULL"));
    return result;
  }

  public int repair() {
    int affected = 0;
    affected +=
        jdbc.update(
            "UPDATE doctor_schedule ds LEFT JOIN (SELECT schedule_id,COUNT(*) cnt FROM appointment"
                + " WHERE status IN (1,2,3,4) GROUP BY schedule_id) x ON x.schedule_id=ds.id SET"
                + " ds.booked_count=COALESCE(x.cnt,0) WHERE ds.booked_count<>COALESCE(x.cnt,0)");
    affected +=
        jdbc.update(
            "UPDATE schedule_slot s LEFT JOIN appointment a ON a.slot_id=s.id AND a.status IN"
                + " (1,2,3,4) SET s.status=0 WHERE s.status=1 AND a.id IS NULL");
    affected +=
        jdbc.update(
            "UPDATE payment_record p JOIN appointment a ON a.id=p.appointment_id SET"
                + " p.status=5,p.refunded_at=COALESCE(p.refunded_at,CURRENT_TIMESTAMP),p.refund_reason=COALESCE(p.refund_reason,'管理员一致性修复')"
                + " WHERE p.status=2 AND a.status IN (6,7,8,9)");
    return affected;
  }

  public void insertOperationLog(long userId, String description) {
    jdbc.update(
        "INSERT INTO operation_log(user_id,operation_type,target_type,description) VALUES(?,?,?,?)",
        userId,
        "REPAIR_DATA_CONSISTENCY",
        "system",
        description);
  }

  private long count(String sql) {
    Long value = jdbc.queryForObject(sql, Long.class);
    return value == null ? 0 : value;
  }
}
