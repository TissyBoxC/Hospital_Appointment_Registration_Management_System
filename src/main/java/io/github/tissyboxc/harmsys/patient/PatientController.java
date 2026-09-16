package io.github.tissyboxc.harmsys.patient;

import io.github.tissyboxc.harmsys.users.UserRegistrationException;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patient/profile")
/** 患者查看和更新个人资料的接口。 */
public class PatientController {
  private final JdbcTemplate jdbc;

  public PatientController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping
  public Object get(HttpServletRequest r) {
    var u = SessionAuth.require(r);
    if (u.patient_id() == null
        || u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("PATIENT")))
      throw new UserRegistrationException(403, "当前账号不是患者");
    return jdbc.queryForMap(
        "SELECT"
            + " id,user_id,real_name,id_card,gender,birthday,phone,address,emergency_contact,emergency_phone"
            + " FROM patient WHERE id=? AND deleted=0",
        u.patient_id());
  }

  @PutMapping
  public Object update(@Valid @RequestBody UpdateRequest x, HttpServletRequest r) {
    var u = SessionAuth.require(r);
    long id = u.patient_id() == null ? 0 : u.patient_id();
    if (id == 0 || u.role_codes().stream().noneMatch(code -> code.equalsIgnoreCase("PATIENT")))
      throw new UserRegistrationException(403, "当前账号不是患者");
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
        "UPDATE_PATIENT_PROFILE",
        id,
        "患者修改个人资料",
        r.getRemoteAddr());
    return get(r);
  }

  /** 患者可自行修改的联系方式和紧急联系人信息。 */
  public record UpdateRequest(
      @NotBlank String real_name,
      @NotBlank String phone,
      String address,
      String emergency_contact,
      String emergency_phone) {}
}
