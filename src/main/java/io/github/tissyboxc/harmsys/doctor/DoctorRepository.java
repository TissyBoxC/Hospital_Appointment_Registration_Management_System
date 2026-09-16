package io.github.tissyboxc.harmsys.doctor;

import io.github.tissyboxc.harmsys.doctor.dto.*;
import io.github.tissyboxc.harmsys.users.UserRegistrationException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
/** 医生资料、排班和时间段的数据访问层。 */
public class DoctorRepository {
  private final JdbcTemplate jdbcTemplate;

  public DoctorRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Optional<DoctorProfileResult> findProfile(long doctorId) {
    return jdbcTemplate
        .query(
            "SELECT"
                + " id,user_id,department_id,doctor_no,real_name,title,specialty,introduction,avatar_url,consultation_fee,status"
                + " FROM doctor WHERE id=? AND deleted=0",
            (rs, n) ->
                new DoctorProfileResult(
                    rs.getLong("id"),
                    rs.getLong("user_id"),
                    rs.getLong("department_id"),
                    rs.getString("doctor_no"),
                    rs.getString("real_name"),
                    rs.getString("title"),
                    rs.getString("specialty"),
                    rs.getString("introduction"),
                    rs.getString("avatar_url"),
                    rs.getBigDecimal("consultation_fee"),
                    rs.getInt("status")),
            doctorId)
        .stream()
        .findFirst();
  }

  public void updateProfile(long doctorId, DoctorProfileUpdateRequest r) {
    jdbcTemplate.update(
        "UPDATE doctor SET"
            + " real_name=?,title=?,specialty=?,introduction=?,avatar_url=?,consultation_fee=?"
            + " WHERE id=? AND deleted=0",
        r.real_name(),
        r.title(),
        r.specialty(),
        r.introduction(),
        r.avatar_url(),
        r.consultation_fee(),
        doctorId);
  }

  public Optional<Long> findDoctorDepartment(long doctorId) {
    return jdbcTemplate
        .query(
            "SELECT department_id FROM doctor WHERE id=? AND deleted=0",
            (rs, n) -> rs.getLong(1),
            doctorId)
        .stream()
        .findFirst();
  }

  public boolean doctorEnabled(long doctorId) {
    return count("SELECT COUNT(*) FROM doctor WHERE id=? AND status=1 AND deleted=0", doctorId) > 0;
  }

  public boolean departmentMatchesDoctor(long doctorId, long departmentId) {
    return count(
            "SELECT COUNT(*) FROM doctor WHERE id=? AND department_id=? AND status=1 AND deleted=0",
            doctorId,
            departmentId)
        > 0;
  }

  public boolean scheduleBelongsTo(long scheduleId, long doctorId) {
    return count(
            "SELECT COUNT(*) FROM doctor_schedule WHERE id=? AND doctor_id=?", scheduleId, doctorId)
        > 0;
  }

  public Optional<ScheduleResult> findSchedule(long id) {
    return jdbcTemplate
        .query(
            "SELECT"
                + " id,doctor_id,department_id,schedule_date,period,start_time,end_time,total_count,booked_count,fee,status,remark"
                + " FROM doctor_schedule WHERE id=?",
            this::mapSchedule,
            id)
        .stream()
        .findFirst();
  }

  public List<ScheduleResult> findSchedules(long doctorId) {
    return jdbcTemplate.query(
        "SELECT"
            + " id,doctor_id,department_id,schedule_date,period,start_time,end_time,total_count,booked_count,fee,status,remark"
            + " FROM doctor_schedule WHERE doctor_id=? ORDER BY schedule_date,start_time",
        this::mapSchedule,
        doctorId);
  }

  public List<ScheduleResult> findAllSchedules() {
    return jdbcTemplate.query(
        "SELECT"
            + " id,doctor_id,department_id,schedule_date,period,start_time,end_time,total_count,booked_count,fee,status,remark"
            + " FROM doctor_schedule ORDER BY schedule_date,start_time",
        this::mapSchedule);
  }

  public long insertSchedule(ScheduleRequest r) {
    KeyHolder kh = new GeneratedKeyHolder();
    int rows =
        jdbcTemplate.update(
            c -> {
              PreparedStatement ps =
                  c.prepareStatement(
                      "INSERT INTO"
                          + " doctor_schedule(doctor_id,department_id,schedule_date,period,start_time,end_time,total_count,booked_count,fee,status,remark)"
                          + " VALUES(?,?,?,?,?,?,?,0,?,0,?)",
                      Statement.RETURN_GENERATED_KEYS);
              ps.setLong(1, r.doctor_id());
              ps.setLong(2, r.department_id());
              ps.setObject(3, r.schedule_date());
              ps.setInt(4, r.period());
              ps.setObject(5, r.start_time());
              ps.setObject(6, r.end_time());
              ps.setInt(7, r.total_count());
              ps.setBigDecimal(8, r.fee());
              ps.setString(9, r.remark());
              return ps;
            },
            kh);
    if (rows != 1 || kh.getKey() == null) throw new IllegalStateException("创建排班失败");
    return kh.getKey().longValue();
  }

  public void updateSchedule(long id, ScheduleRequest r) {
    if (jdbcTemplate.update(
            "UPDATE doctor_schedule SET"
                + " department_id=?,schedule_date=?,period=?,start_time=?,end_time=?,total_count=?,fee=?,remark=?"
                + " WHERE id=? AND booked_count=0",
            r.department_id(),
            r.schedule_date(),
            r.period(),
            r.start_time(),
            r.end_time(),
            r.total_count(),
            r.fee(),
            r.remark(),
            id)
        != 1) throw new UserRegistrationException(409, "排班不存在，或已有预约不能修改");
  }

  public void deleteSchedule(long id) {
    if (jdbcTemplate.update("DELETE FROM doctor_schedule WHERE id=? AND booked_count=0", id) != 1)
      throw new IllegalStateException("已有预约的排班不可删除");
  }

  public List<SlotResult> findSlots(long scheduleId) {
    return jdbcTemplate.query(
        "SELECT id,schedule_id,slot_no,start_time,end_time,status FROM schedule_slot WHERE"
            + " schedule_id=? ORDER BY slot_no",
        this::mapSlot,
        scheduleId);
  }

  public long insertSlot(long scheduleId, SlotRequest r) {
    KeyHolder kh = new GeneratedKeyHolder();
    int rows =
        jdbcTemplate.update(
            c -> {
              PreparedStatement ps =
                  c.prepareStatement(
                      "INSERT INTO schedule_slot(schedule_id,slot_no,start_time,end_time,status)"
                          + " VALUES(?,?,?,?,0)",
                      Statement.RETURN_GENERATED_KEYS);
              ps.setLong(1, scheduleId);
              ps.setInt(2, r.slot_no());
              ps.setObject(3, r.start_time());
              ps.setObject(4, r.end_time());
              return ps;
            },
            kh);
    if (rows != 1 || kh.getKey() == null) throw new IllegalStateException("创建时间段失败");
    return kh.getKey().longValue();
  }

  public boolean slotOverlaps(long scheduleId, java.time.LocalTime start, java.time.LocalTime end) {
    return count(
            "SELECT COUNT(*) FROM schedule_slot WHERE schedule_id=? AND start_time < ? AND end_time"
                + " > ?",
            scheduleId,
            end,
            start)
        > 0;
  }

  public boolean slotNumberExists(long scheduleId, int slotNo) {
    return count(
            "SELECT COUNT(*) FROM schedule_slot WHERE schedule_id=? AND slot_no=?",
            scheduleId,
            slotNo)
        > 0;
  }

  public void updateSlotStatus(long slotId, int status) {
    if (jdbcTemplate.update("UPDATE schedule_slot SET status=? WHERE id=?", status, slotId) != 1)
      throw new IllegalStateException("时间段不存在");
  }

  public Optional<SlotResult> findSlot(long slotId) {
    return jdbcTemplate
        .query(
            "SELECT id,schedule_id,slot_no,start_time,end_time,status FROM schedule_slot WHERE"
                + " id=?",
            this::mapSlot,
            slotId)
        .stream()
        .findFirst();
  }

  public void writeLog(
      long userId, String type, String target, Long id, String description, String ip) {
    jdbcTemplate.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,?,?,?,?)",
        userId,
        type,
        target,
        id,
        description,
        ip);
  }

  private long count(String sql, Object... args) {
    Long v = jdbcTemplate.queryForObject(sql, Long.class, args);
    return v == null ? 0 : v;
  }

  private ScheduleResult mapSchedule(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
    return new ScheduleResult(
        rs.getLong("id"),
        rs.getLong("doctor_id"),
        rs.getLong("department_id"),
        rs.getObject("schedule_date", LocalDate.class),
        rs.getInt("period"),
        rs.getObject("start_time", java.time.LocalTime.class),
        rs.getObject("end_time", java.time.LocalTime.class),
        rs.getInt("total_count"),
        rs.getInt("booked_count"),
        rs.getBigDecimal("fee"),
        rs.getInt("status"),
        rs.getString("remark"));
  }

  private SlotResult mapSlot(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
    return new SlotResult(
        rs.getLong("id"),
        rs.getLong("schedule_id"),
        rs.getInt("slot_no"),
        rs.getObject("start_time", java.time.LocalTime.class),
        rs.getObject("end_time", java.time.LocalTime.class),
        rs.getInt("status"));
  }
}
