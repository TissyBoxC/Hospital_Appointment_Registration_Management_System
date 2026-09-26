package io.github.tissyboxc.harmsys.clinical.controller;

import io.github.tissyboxc.harmsys.clinical.service.MedicalAttachmentService;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/medical/attachments")
/** 患者、医生和管理员使用的检查报告/检验结果/病历附件接口。 */
public class MedicalAttachmentController {
  private final MedicalAttachmentService service;
  //根目录
  private final Path root =
      Paths.get(System.getProperty("harms.upload-dir", "uploads")).toAbsolutePath().normalize();

  public MedicalAttachmentController(MedicalAttachmentService service) {
    this.service = service;
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
    //文件校验
    if (file == null || file.isEmpty()) throw new UserRegistrationException(422, "文件不能为空");
    if (file.getSize() > 10 * 1024 * 1024) throw new UserRegistrationException(422, "文件不能超过10MB");
    Files.createDirectories(root);
    //开始上传文件
    String stored = UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename());
    Path target = root.resolve(stored).normalize();
    if (!target.startsWith(root)) throw new UserRegistrationException(422, "非法文件名");
    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
    return service.save(
        u,
        patient_id,
        visit_id,
        attachment_type,
        file.getOriginalFilename(),
        stored,
        target.toString(),
        file.getContentType(),
        file.getSize(),
        description);
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
    return service.list(u, patient_id, visit_id);
  }

  /**
   * 根据文件ID查询附件
   * @param id 文件ID
   */
  @GetMapping("/{id}")
  public Map<String, Object> get(@PathVariable long id, HttpServletRequest request) {
    return service.get(SessionAuth.require(request), id);
  }

  /**
   * 删除文件
   */
  @DeleteMapping("/{id}")
  public void delete(@PathVariable long id, HttpServletRequest request) {
    Map<String, Object> attachment = service.delete(SessionAuth.require(request), id);
    try {
      Files.deleteIfExists(Paths.get(String.valueOf(attachment.get("file_path"))));
    } catch (IOException ignored) {
    }
  }

  /**
   * 格式化文件名
   * @param s 文件名
   */
  private String sanitize(String s) {
    if (s == null || s.isBlank()) return "file";
    return s.replaceAll("[^A-Za-z0-9._-]", "_");
  }
}


