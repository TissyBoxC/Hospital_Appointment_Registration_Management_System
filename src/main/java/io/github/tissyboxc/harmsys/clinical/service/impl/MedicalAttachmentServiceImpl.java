package io.github.tissyboxc.harmsys.clinical.service.impl;

import io.github.tissyboxc.harmsys.clinical.service.MedicalAttachmentService;

import io.github.tissyboxc.harmsys.clinical.entity.MedicalAttachment;
import io.github.tissyboxc.harmsys.clinical.mapper.MedicalAttachmentMapper;
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
public class MedicalAttachmentServiceImpl implements MedicalAttachmentService {
  private final MedicalAttachmentMapper mapper;

  public MedicalAttachmentServiceImpl(MedicalAttachmentMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
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
    MedicalAttachment attachment = new MedicalAttachment();
    attachment.setPatientId(patientId);
    attachment.setVisitId(visitId);
    attachment.setUploaderUserId(user.user_id());
    attachment.setAttachmentType(attachmentType);
    attachment.setOriginalName(originalName);
    attachment.setStoredName(storedName);
    attachment.setFilePath(filePath);
    attachment.setContentType(contentType);
    attachment.setFileSize(fileSize);
    attachment.setDescription(description);
    mapper.insert(attachment);
    return mapper.selectDetail(attachment.getId());
  }

  @Transactional(readOnly = true)
  @Override
  public List<Map<String, Object>> list(
      AuthenticatedUser user, Long requestedPatientId, Long visitId) {
    Long patientId = requestedPatientId == null ? user.patient_id() : requestedPatientId;
    if (patientId == null) throw new UserRegistrationException(422, "patient_id不能为空");
    authorizePatient(user, patientId);
    return mapper.selectByPatient(patientId, visitId);
  }

  @Transactional(readOnly = true)
  @Override
  public Map<String, Object> get(AuthenticatedUser user, long id) {
    Map<String, Object> attachment = requireAttachment(id);
    authorizePatient(user, ((Number) attachment.get("patient_id")).longValue());
    return attachment;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> delete(AuthenticatedUser user, long id) {
    Map<String, Object> attachment = requireAttachment(id);
    authorizePatient(user, ((Number) attachment.get("patient_id")).longValue());
    if (!Objects.equals(
            user.user_id(), ((Number) attachment.get("uploader_user_id")).longValue())
        && user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "无权删除附件");
    mapper.deleteById(id);
    return attachment;
  }

  private Map<String, Object> requireAttachment(long id) {
    Map<String, Object> attachment = mapper.selectDetail(id);
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
    if (mapper.countActivePatient(patientId) == 0)
      throw new UserRegistrationException(404, "患者不存在");
  }
}
