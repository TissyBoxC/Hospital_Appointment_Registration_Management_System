package io.github.tissyboxc.harmsys.clinical;

import io.github.tissyboxc.harmsys.users.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.users.UserRegistrationException;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/medical/attachments")
/** 患者、医生和管理员使用的检查报告/检验结果/病历附件接口。 */
public class MedicalAttachmentController {
  private final JdbcTemplate jdbc;
  //根目录
  private final Path root =
      Paths.get(System.getProperty("harms.upload-dir", "uploads")).toAbsolutePath().normalize();

  public MedicalAttachmentController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * 上传附件
   * @param file 文件
   * @param patient_id 患者ID
   * @param visit_id 就诊记录ID
   * @param attachment_type 附件类型
   * @param description 描述
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String, Object> upload(
      @RequestPart("file") MultipartFile file,
      @RequestParam long patient_id,
      @RequestParam(required = false) Long visit_id,
      @RequestParam(defaultValue = "MEDICAL_RECORD") String attachment_type,
      @RequestParam(required = false) String description,
      HttpServletRequest request)
      throws IOException {
    //确认是否登录
    AuthenticatedUser u = SessionAuth.require(request);
    authorizePatient(u, patient_id);
    //文件校验
    if (file == null || file.isEmpty()) throw new UserRegistrationException(422, "文件不能为空");
    if (file.getSize() > 10 * 1024 * 1024) throw new UserRegistrationException(422, "文件不能超过10MB");
    Files.createDirectories(root);
    //开始上传文件
    String stored = UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename());
    Path target = root.resolve(stored).normalize();
    if (!target.startsWith(root)) throw new UserRegistrationException(422, "非法文件名");
    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
    jdbc.update(
        "INSERT INTO"
            + " medical_attachment(patient_id,visit_id,uploader_user_id,attachment_type,original_name,stored_name,file_path,content_type,file_size,description)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?)",
        patient_id,
        visit_id,
        u.user_id(),
        attachment_type,
        file.getOriginalFilename(),
        stored,
        target.toString(),
        file.getContentType(),
        file.getSize(),
        description);
    long id =
        jdbc.queryForObject(
            "SELECT id FROM medical_attachment WHERE stored_name=?", Long.class, stored);
    return one("SELECT * FROM medical_attachment WHERE id=?", id);
  }

  /**
   * 根据患者ID查询文件列表
   * @param patient_id 患者ID
   * @param visit_id 就诊记录ID
   */
  @GetMapping
  public List<Map<String, Object>> list(
      @RequestParam(required = false) Long patient_id,
      @RequestParam(required = false) Long visit_id,
      HttpServletRequest request) {
    AuthenticatedUser u = SessionAuth.require(request);
    if (patient_id == null) patient_id = u.patient_id();
    if (patient_id == null) throw new UserRegistrationException(422, "patient_id不能为空");
    authorizePatient(u, patient_id);
    if (visit_id == null)
      return jdbc.queryForList(
          "SELECT"
              + " id,patient_id,visit_id,uploader_user_id,attachment_type,original_name,content_type,file_size,description,created_at"
              + " FROM medical_attachment WHERE patient_id=? ORDER BY created_at DESC",
          patient_id);
    return jdbc.queryForList(
        "SELECT"
            + " id,patient_id,visit_id,uploader_user_id,attachment_type,original_name,content_type,file_size,description,created_at"
            + " FROM medical_attachment WHERE patient_id=? AND visit_id=? ORDER BY created_at DESC",
        patient_id,
        visit_id);
  }

  /**
   * 根据文件ID查询附件
   * @param id 文件ID
   */
  @GetMapping("/{id}")
  public Map<String, Object> get(@PathVariable long id, HttpServletRequest request) {
    Map<String, Object> m = one("SELECT * FROM medical_attachment WHERE id=?", id);
    if (m == null) throw new UserRegistrationException(404, "附件不存在");
    AuthenticatedUser u = SessionAuth.require(request);
    authorizePatient(u, ((Number) m.get("patient_id")).longValue());
    return m;
  }

  /**
   * 删除文件
   */
  @DeleteMapping("/{id}")
  public void delete(@PathVariable long id, HttpServletRequest request) {
    Map<String, Object> m = get(id, request);
    AuthenticatedUser u = SessionAuth.require(request);
    //验证角色为本人或管理员
    if (!Objects.equals(u.user_id(), ((Number) m.get("uploader_user_id")).longValue())
        && !u.role_codes().stream().anyMatch(x -> x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "无权删除附件");
    jdbc.update("DELETE FROM medical_attachment WHERE id=?", id);
    try {
      Files.deleteIfExists(Paths.get(String.valueOf(m.get("file_path"))));
    } catch (IOException ignored) {
    }
  }

  /**
   * 认证患者信息
   * @param u 已登录角色信息
   * @param patientId 患者ID
   */
  private void authorizePatient(AuthenticatedUser u, long patientId) {
    //确认角色是患者本人/医生/管理员
    if (!u.role_codes().stream()
        .anyMatch(
            x ->
                x.equalsIgnoreCase("ADMIN")
                    || x.equalsIgnoreCase("DOCTOR")
                    || x.equalsIgnoreCase("PATIENT")))
      throw new SessionAuthenticationException(403, "无权访问附件");
    if (u.role_codes().stream().anyMatch(x -> x.equalsIgnoreCase("PATIENT"))
        && !Objects.equals(u.patient_id(), patientId))
      throw new SessionAuthenticationException(403, "只能访问自己的附件");
    if (jdbc.queryForObject(
            "SELECT COUNT(*) FROM patient WHERE id=? AND deleted=0", Long.class, patientId)
        == 0) throw new UserRegistrationException(404, "患者不存在");
  }

  /**
   * 格式化文件名
   * @param s 文件名
   */
  private String sanitize(String s) {
    if (s == null || s.isBlank()) return "file";
    return s.replaceAll("[^A-Za-z0-9._-]", "_");
  }

  private Map<String, Object> one(String sql, Object... a) {
    return jdbc.query(sql, rs -> rs.next() ? row(rs) : null, a);
  }

  private Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
    Map<String, Object> m = new LinkedHashMap<>();
    var md = rs.getMetaData();
    for (int i = 1; i <= md.getColumnCount(); i++) m.put(md.getColumnLabel(i), rs.getObject(i));
    return m;
  }
}
