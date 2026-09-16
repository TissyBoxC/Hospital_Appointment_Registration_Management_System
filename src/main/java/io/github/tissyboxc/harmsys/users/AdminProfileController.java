package io.github.tissyboxc.harmsys.users;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
/** 管理员查询或修改患者、医生资料和状态的接口。 */
public class AdminProfileController {
  private final JdbcTemplate jdbc;

  public AdminProfileController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser check(HttpServletRequest r) {
    var u = io.github.tissyboxc.harmsys.users.sessions.SessionAuth.require(r);
    if (u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "只有管理员可以执行该操作");
    return u;
  }

  @GetMapping("/patients/{id}")
  public Object getPatient(@PathVariable long id, HttpServletRequest r) {
    check(r);
    try {
      return jdbc.queryForMap("SELECT * FROM patient WHERE id=? AND deleted=0", id);
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
      throw new UserRegistrationException(404, "患者资料不存在");
    }
  }

  @PutMapping("/patients/{id}")
  public Object patient(
      @PathVariable long id, @Valid @RequestBody PatientUpdate x, HttpServletRequest r) {
    var u = check(r);
    if (jdbc.update(
            "UPDATE patient SET real_name=?,phone=?,address=?,emergency_contact=?,emergency_phone=?"
                + " WHERE id=? AND deleted=0",
            x.real_name(),
            x.phone(),
            x.address(),
            x.emergency_contact(),
            x.emergency_phone(),
            id)
        != 1) throw new UserRegistrationException(404, "患者资料不存在");
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,'patient',?,?,?)",
        u.user_id(),
        "ADMIN_UPDATE_PATIENT",
        id,
        "管理员修改患者资料",
        r.getRemoteAddr());
    return jdbc.queryForMap("SELECT * FROM patient WHERE id=?", id);
  }

  @GetMapping("/doctors/{id}")
  public Object getDoctor(@PathVariable long id, HttpServletRequest r) {
    check(r);
    try {
      return jdbc.queryForMap("SELECT * FROM doctor WHERE id=? AND deleted=0", id);
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
      throw new UserRegistrationException(404, "医生资料不存在");
    }
  }

  @PutMapping("/doctors/{id}")
  public Object doctor(
      @PathVariable long id, @Valid @RequestBody DoctorUpdate x, HttpServletRequest r) {
    var u = check(r);
    if (jdbc.update(
            "UPDATE doctor SET"
                + " department_id=?,real_name=?,title=?,specialty=?,introduction=?,avatar_url=?,consultation_fee=?"
                + " WHERE id=? AND deleted=0",
            x.department_id(),
            x.real_name(),
            x.title(),
            x.specialty(),
            x.introduction(),
            x.avatar_url(),
            x.consultation_fee(),
            id)
        != 1) throw new UserRegistrationException(404, "医生资料不存在");
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,'doctor',?,?,?)",
        u.user_id(),
        "ADMIN_UPDATE_DOCTOR",
        id,
        "管理员修改医生资料",
        r.getRemoteAddr());
    return jdbc.queryForMap("SELECT * FROM doctor WHERE id=?", id);
  }

  @PutMapping("/doctors/{id}/status")
  public void doctorStatus(
      @PathVariable long id, @Valid @RequestBody Status x, HttpServletRequest r) {
    var u = check(r);
    if (jdbc.update("UPDATE doctor SET status=? WHERE id=? AND deleted=0", x.status(), id) != 1)
      throw new UserRegistrationException(404, "医生资料不存在");
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,'doctor',?,?,?)",
        u.user_id(),
        "ADMIN_UPDATE_DOCTOR_STATUS",
        id,
        "管理员修改医生执业状态",
        r.getRemoteAddr());
  }

  /** 管理员可修改的患者资料字段。 */
  public record PatientUpdate(
      @NotBlank String real_name,
      @NotBlank String phone,
      String address,
      String emergency_contact,
      String emergency_phone) {}

  /** 管理员可修改的医生资料字段。 */
  public record DoctorUpdate(
      @NotNull Long department_id,
      @NotBlank String real_name,
      String title,
      String specialty,
      String introduction,
      String avatar_url,
      @NotNull @DecimalMin("0.00") java.math.BigDecimal consultation_fee) {}

  /** 资料启用或禁用状态请求。 */
  public record Status(@NotNull @Min(0) @Max(1) Integer status) {}
}
