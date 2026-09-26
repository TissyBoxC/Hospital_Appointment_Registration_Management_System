package io.github.tissyboxc.harmsys.clinical.service;

import io.github.tissyboxc.harmsys.clinical.repository.MedicalAttachmentRepository;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 医疗附件权限与元数据业务逻辑。 */
@Service
public class MedicalAttachmentService {
  private final MedicalAttachmentRepository repository;

  public MedicalAttachmentService(MedicalAttachmentRepository repository) {
    this.repository = repository;
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> save(
      AuthenticatedUser user,
      long patientId,
      Long visitId,
      String attachmentType,
      String originalName,
      String storedName,
      String filePath,
      String contentType,
      long fileSize,
      String description) {
    authorizePatient(user, patientId);
    long id =
        repository.insert(
            patientId,
            visitId,
            user.user_id(),
            attachmentType,
            originalName,
            storedName,
            filePath,
            contentType,
            fileSize,
            description);
    return repository.findById(id);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> list(
      AuthenticatedUser user, Long requestedPatientId, Long visitId) {
    Long patientId = requestedPatientId == null ? user.patient_id() : requestedPatientId;
    if (patientId == null) throw new UserRegistrationException(422, "patient_id不能为空");
    authorizePatient(user, patientId);
    return repository.listByPatient(patientId, visitId);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> get(AuthenticatedUser user, long id) {
    Map<String, Object> attachment = requireAttachment(id);
    authorizePatient(user, ((Number) attachment.get("patient_id")).longValue());
    return attachment;
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> delete(AuthenticatedUser user, long id) {
    Map<String, Object> attachment = requireAttachment(id);
    authorizePatient(user, ((Number) attachment.get("patient_id")).longValue());
    if (!Objects.equals(
            user.user_id(), ((Number) attachment.get("uploader_user_id")).longValue())
        && user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "无权删除附件");
    repository.delete(id);
    return attachment;
  }

  private Map<String, Object> requireAttachment(long id) {
    Map<String, Object> attachment = repository.findById(id);
    if (attachment == null) throw new UserRegistrationException(404, "附件不存在");
    return attachment;
  }

  private void authorizePatient(AuthenticatedUser user, long patientId) {
    if (user.role_codes().stream()
        .noneMatch(
            role ->
                role.equalsIgnoreCase("ADMIN")
                    || role.equalsIgnoreCase("DOCTOR")
                    || role.equalsIgnoreCase("PATIENT")))
      throw new SessionAuthenticationException(403, "无权访问附件");
    if (user.role_codes().stream().anyMatch("PATIENT"::equalsIgnoreCase)
        && !Objects.equals(user.patient_id(), patientId))
      throw new SessionAuthenticationException(403, "只能访问自己的附件");
    if (!repository.patientExists(patientId))
      throw new UserRegistrationException(404, "患者不存在");
  }
}
