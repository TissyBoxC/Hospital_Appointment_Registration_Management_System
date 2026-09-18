package io.github.tissyboxc.harmsys.pharmacy;

import io.github.tissyboxc.harmsys.users.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.users.UserRegistrationException;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** 药房端处方查询和发药确认。当前不处理库存，仅维护处方流转状态。 */
@RestController
@RequestMapping("/api/pharmacy")
/** 药房查看处方并更新发药状态的接口。 */
public class PharmacyController {
  private final JdbcTemplate jdbc;

  public PharmacyController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * 查询所有处方
   * @param status 状态
   */
  @GetMapping("/prescriptions")
  public List<Map<String, Object>> list(
      @RequestParam(required = false) Integer status, HttpServletRequest request) {
    //身份验证
    AuthenticatedUser operator = requirePharmacy(request);
    //查询所有状态的处方
    if (status == null) {
      return jdbc.queryForList(
          "SELECT p.*,v.patient_id,pt.real_name patient_name,pt.phone patient_phone,d.real_name"
              + " doctor_name FROM prescription p JOIN medical_visit v ON v.id=p.visit_id JOIN"
              + " patient pt ON pt.id=v.patient_id JOIN doctor d ON d.id=p.doctor_id WHERE p.status"
              + " IN (2,3) ORDER BY p.created_at DESC");
    }
    //status不合法
    if (status < 1 || status > 3) throw new UserRegistrationException(422, "处方状态只能为1到3");
    //查询指定状态的处方
    return jdbc.queryForList(
        "SELECT p.*,v.patient_id,pt.real_name patient_name,pt.phone patient_phone,d.real_name"
            + " doctor_name FROM prescription p JOIN medical_visit v ON v.id=p.visit_id JOIN"
            + " patient pt ON pt.id=v.patient_id JOIN doctor d ON d.id=p.doctor_id WHERE p.status=?"
            + " ORDER BY p.created_at DESC",
        status);
  }

  /**
   * 根据ID查询处方
   * @param id 处方ID
   */
  @GetMapping("/prescriptions/{id}")
  public Map<String, Object> get(@PathVariable long id, HttpServletRequest request) {
    requirePharmacy(request);
    Map<String, Object> result =
        one(
            "SELECT p.*,v.patient_id,pt.real_name patient_name,pt.phone patient_phone,d.real_name"
                + " doctor_name FROM prescription p JOIN medical_visit v ON v.id=p.visit_id JOIN"
                + " patient pt ON pt.id=v.patient_id JOIN doctor d ON d.id=p.doctor_id WHERE"
                + " p.id=?",
            id);
    if (result == null) throw new UserRegistrationException(404, "处方不存在");
    result.put(
        "items",
        jdbc.queryForList(
            "SELECT * FROM prescription_item WHERE prescription_id=? ORDER BY id", id));
    return result;
  }

  /**
   *处方标记已取药
   */
  @PostMapping("/prescriptions/{id}/dispense")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional(rollbackFor = Exception.class)
  public void dispense(@PathVariable long id, HttpServletRequest request) {
    //身份验证
    AuthenticatedUser operator = requirePharmacy(request);
    if (jdbc.update("UPDATE prescription SET status=3 WHERE id=? AND status=2", id) != 1) {
      throw new UserRegistrationException(409, "处方不存在或当前不能标记为已取药");
    }
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,?,?,?,?)",
        operator.user_id(),
        "DISPENSE_PRESCRIPTION",
        "prescription",
        id,
        "药房确认患者已取药",
        request.getRemoteAddr());
  }

  private AuthenticatedUser requirePharmacy(HttpServletRequest request) {
    AuthenticatedUser user = SessionAuth.require(request);
    if (user.role_codes().stream()
        .noneMatch(x -> x.equalsIgnoreCase("PHARMACY") || x.equalsIgnoreCase("ADMIN"))) {
      throw new SessionAuthenticationException(403, "没有药房操作权限");
    }
    return user;
  }

  private Map<String, Object> one(String sql, Object... args) {
    return jdbc.query(sql, rs -> rs.next() ? row(rs) : null, args);
  }

  private Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
    Map<String, Object> map = new LinkedHashMap<>();
    var md = rs.getMetaData();
    for (int i = 1; i <= md.getColumnCount(); i++) map.put(md.getColumnLabel(i), rs.getObject(i));
    return map;
  }
}
