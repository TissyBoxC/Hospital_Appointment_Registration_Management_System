package io.github.tissyboxc.harmsys.doctor.repository;

import io.github.tissyboxc.harmsys.doctor.dto.ScheduleRequestResult;
import io.github.tissyboxc.harmsys.doctor.dto.ScheduleRequestSubmit;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** 医生排班申请、审核和正式排班变更的数据访问层。 */
@Repository
public class ScheduleRequestRepository {
  private final JdbcTemplate jdbc;

  public ScheduleRequestRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public boolean departmentMatchesDoctor(long doctorId, long departmentId) {
    return count(
            "SELECT COUNT(*) FROM doctor WHERE id=? AND department_id=? AND status=1 AND deleted=0",
            doctorId,
            departmentId)
        > 0;
  }

  public Map<String, Object> findScheduleForDoctor(long scheduleId, long doctorId) {
    return one("SELECT * FROM doctor_schedule WHERE id=? AND doctor_id=?", scheduleId, doctorId);
  }

  public long countPendingConflict(
      long doctorId, Long targetScheduleId, LocalDate scheduleDate, int period) {
    String sql =
        "SELECT COUNT(*) FROM doctor_schedule_request WHERE doctor_id=? AND status=0"
            + " AND schedule_date=? AND period=?";
    if (targetScheduleId == null)
      return count(sql, doctorId, scheduleDate, period);
    return count(
        sql + " AND (target_schedule_id IS NULL OR target_schedule_id<>?)",
        doctorId,
        scheduleDate,
        period,
        targetScheduleId);
  }

  public long countFormalScheduleConflict(
      long doctorId, Long targetScheduleId, LocalDate scheduleDate, int period) {
    if (targetScheduleId == null)
      return count(
          "SELECT COUNT(*) FROM doctor_schedule WHERE doctor_id=? AND schedule_date=? AND period=?",
          doctorId,
          scheduleDate,
          period);
    return count(
        "SELECT COUNT(*) FROM doctor_schedule WHERE doctor_id=? AND schedule_date=? AND period=? AND id<>?",
        doctorId,
        scheduleDate,
        period,
        targetScheduleId);
  }

  public long insertRequest(
      long doctorId,
      long departmentId,
      Long targetScheduleId,
      int type,
      ScheduleRequestSubmit body,
      long requestedByUserId) {
    KeyHolder holder = new GeneratedKeyHolder();
    int rows =
        jdbc.update(
            connection -> {
              PreparedStatement statement =
                  connection.prepareStatement(
                      "INSERT INTO doctor_schedule_request(doctor_id,department_id,target_schedule_id,"
                          + "request_type,schedule_date,period,start_time,end_time,total_count,fee,remark,"
                          + "status,requested_by_user_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,0,?)",
                      Statement.RETURN_GENERATED_KEYS);
              statement.setLong(1, doctorId);
              statement.setLong(2, departmentId);
              if (targetScheduleId == null) statement.setNull(3, java.sql.Types.BIGINT);
              else statement.setLong(3, targetScheduleId);
              statement.setInt(4, type);
              statement.setObject(5, body.schedule_date());
              statement.setInt(6, body.period());
              statement.setObject(7, body.start_time());
              statement.setObject(8, body.end_time());
              statement.setInt(9, body.total_count());
              statement.setBigDecimal(10, body.fee());
              statement.setString(11, body.remark());
              statement.setLong(12, requestedByUserId);
              return statement;
            },
            holder);
    if (holder.getKey() == null) throw new IllegalStateException("提交排班申请失败");
    return holder.getKey().longValue();
  }

  public List<ScheduleRequestResult> mine(long doctorId, Integer status) {
    String sql =
        "SELECT r.*,d.real_name doctor_name,dp.name department_name,u.username reviewer_name "
            + "FROM doctor_schedule_request r JOIN doctor d ON d.id=r.doctor_id "
            + "JOIN department dp ON dp.id=r.department_id "
            + "LEFT JOIN sys_user u ON u.id=r.reviewed_by_user_id "
            + "WHERE r.doctor_id=?"
            + (status == null ? "" : " AND r.status=?")
            + " ORDER BY r.created_at DESC";
    return status == null
        ? list(sql, doctorId)
        : list(sql, doctorId, status);
  }

  public Map<String, Object> lockRequest(long id) {
    return one("SELECT * FROM doctor_schedule_request WHERE id=? FOR UPDATE", id);
  }

  public Map<String, Object> lockDoctorRequest(long id, long doctorId) {
    return one(
        "SELECT * FROM doctor_schedule_request WHERE id=? AND doctor_id=? FOR UPDATE",
        id,
        doctorId);
  }

  public int cancelRequest(long id) {
    return jdbc.update(
        "UPDATE doctor_schedule_request SET status=3,review_remark='医生取消申请' WHERE id=?",
        id);
  }

  public List<ScheduleRequestResult> reviewList(
      boolean admin,
      boolean departmentManager,
      boolean hasReviewPermission,
      Long operatorDepartmentId,
      long operatorUserId,
      Integer status,
      Long departmentId) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT r.*,d.real_name doctor_name,dp.name department_name,"
                + "u.username reviewer_name FROM doctor_schedule_request r "
                + "JOIN doctor d ON d.id=r.doctor_id JOIN department dp ON dp.id=r.department_id "
                + "LEFT JOIN sys_user u ON u.id=r.reviewed_by_user_id WHERE 1=1");
    List<Object> args = new ArrayList<>();
    if (!admin) {
      sql.append(" AND (");
      if (departmentManager) {
        sql.append(
            "EXISTS (SELECT 1 FROM department_manager dm"
                + " WHERE dm.department_id=r.department_id AND dm.user_id=?)");
        args.add(operatorUserId);
      } else {
        sql.append("1=0");
      }
      if (hasReviewPermission && operatorDepartmentId != null) {
        sql.append(" OR r.department_id=?");
        args.add(operatorDepartmentId);
      }
      sql.append(")");
    }
    if (departmentId != null) {
      sql.append(" AND r.department_id=?");
      args.add(departmentId);
    }
    if (status != null) {
      sql.append(" AND r.status=?");
      args.add(status);
    }
    sql.append(" ORDER BY CASE WHEN r.status=0 THEN 0 ELSE 1 END,r.created_at DESC");
    return list(sql.toString(), args.toArray());
  }

  public boolean isDepartmentManager(long userId, long departmentId) {
    return count(
            "SELECT COUNT(*) FROM department_manager WHERE department_id=? AND user_id=?",
            departmentId,
            userId)
        > 0;
  }

  public int rejectRequest(long id, long operatorUserId, String reviewRemark) {
    return jdbc.update(
        "UPDATE doctor_schedule_request SET status=2,reviewed_by_user_id=?,reviewed_at=CURRENT_TIMESTAMP,"
            + "review_remark=? WHERE id=?",
        operatorUserId,
        reviewRemark,
        id);
  }

  public int approveRequest(long id, long operatorUserId, String reviewRemark, Long scheduleId) {
    return jdbc.update(
        "UPDATE doctor_schedule_request SET status=1,reviewed_by_user_id=?,reviewed_at=CURRENT_TIMESTAMP,"
            + "review_remark=?,applied_schedule_id=? WHERE id=?",
        operatorUserId,
        reviewRemark,
        scheduleId,
        id);
  }

  public long insertSchedule(Map<String, Object> request) {
    KeyHolder holder = new GeneratedKeyHolder();
    int rows =
        jdbc.update(
            connection -> {
              PreparedStatement statement =
                  connection.prepareStatement(
                      "INSERT INTO doctor_schedule(doctor_id,department_id,schedule_date,period,"
                          + "start_time,end_time,total_count,booked_count,fee,status,remark)"
                          + " VALUES(?,?,?,?,?,?,?,0,?,0,?)",
                      Statement.RETURN_GENERATED_KEYS);
              statement.setLong(1, ((Number) request.get("doctor_id")).longValue());
              statement.setLong(2, ((Number) request.get("department_id")).longValue());
              statement.setObject(3, request.get("schedule_date"));
              statement.setInt(4, ((Number) request.get("period")).intValue());
              statement.setObject(5, request.get("start_time"));
              statement.setObject(6, request.get("end_time"));
              statement.setInt(7, ((Number) request.get("total_count")).intValue());
              statement.setBigDecimal(8, (BigDecimal) request.get("fee"));
              statement.setString(9, (String) request.get("remark"));
              return statement;
            },
            holder);
    if (holder.getKey() == null)
      throw new IllegalStateException("审批通过后创建正式排班失败");
    return holder.getKey().longValue();
  }

  public int updateSchedule(long scheduleId, Map<String, Object> request) {
    return jdbc.update(
        "UPDATE doctor_schedule SET department_id=?,schedule_date=?,period=?,start_time=?,"
            + "end_time=?,total_count=?,fee=?,remark=? WHERE id=? AND booked_count=0",
        request.get("department_id"),
        request.get("schedule_date"),
        request.get("period"),
        request.get("start_time"),
        request.get("end_time"),
        request.get("total_count"),
        request.get("fee"),
        request.get("remark"),
        scheduleId);
  }

  public int deleteSchedule(long scheduleId) {
    return jdbc.update(
        "DELETE FROM doctor_schedule WHERE id=? AND booked_count=0", scheduleId);
  }

  public ScheduleRequestResult findRequest(long id) {
    return list(
            "SELECT r.*,d.real_name doctor_name,dp.name department_name,u.username reviewer_name "
                + "FROM doctor_schedule_request r JOIN doctor d ON d.id=r.doctor_id "
                + "JOIN department dp ON dp.id=r.department_id "
                + "LEFT JOIN sys_user u ON u.id=r.reviewed_by_user_id WHERE r.id=?",
            id)
        .stream()
        .findFirst()
        .orElseThrow(
            () ->
                new io.github.tissyboxc.harmsys.common.UserRegistrationException(
                    404, "排班申请不存在"));
  }

  public void insertOperationLog(
      long userId, String type, long id, String description, String ipAddress) {
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,'doctor_schedule_request',?,?,?)",
        userId,
        type,
        id,
        description,
        ipAddress);
  }

  private List<ScheduleRequestResult> list(String sql, Object... args) {
    return jdbc.query(
        sql,
        (rs, rowNum) ->
            new ScheduleRequestResult(
                rs.getLong("id"),
                rs.getLong("doctor_id"),
                rs.getString("doctor_name"),
                rs.getLong("department_id"),
                rs.getString("department_name"),
                nullableLong(rs, "target_schedule_id"),
                rs.getInt("request_type"),
                rs.getObject("schedule_date", LocalDate.class),
                rs.getInt("period"),
                rs.getObject("start_time", LocalTime.class),
                rs.getObject("end_time", LocalTime.class),
                rs.getInt("total_count"),
                rs.getBigDecimal("fee"),
                rs.getString("remark"),
                rs.getInt("status"),
                nullableLong(rs, "requested_by_user_id"),
                nullableLong(rs, "reviewed_by_user_id"),
                rs.getString("reviewer_name"),
                rs.getObject("reviewed_at", LocalDateTime.class),
                rs.getString("review_remark"),
                nullableLong(rs, "applied_schedule_id"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class)),
        args);
  }

  private long count(String sql, Object... args) {
    Long value = jdbc.queryForObject(sql, Long.class, args);
    return value == null ? 0 : value;
  }

  private Map<String, Object> one(String sql, Object... args) {
    return jdbc.query(sql, rs -> rs.next() ? row(rs) : null, args);
  }

  private Map<String, Object> row(ResultSet rs) throws SQLException {
    Map<String, Object> map = new LinkedHashMap<>();
    var metadata = rs.getMetaData();
    for (int i = 1; i <= metadata.getColumnCount(); i++)
      map.put(metadata.getColumnLabel(i), rs.getObject(i));
    return map;
  }

  private Long nullableLong(ResultSet rs, String column) throws SQLException {
    long value = rs.getLong(column);
    return rs.wasNull() ? null : value;
  }
}
