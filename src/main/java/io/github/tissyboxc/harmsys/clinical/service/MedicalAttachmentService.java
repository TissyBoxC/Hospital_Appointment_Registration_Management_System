package io.github.tissyboxc.harmsys.clinical.service;

import io.github.tissyboxc.harmsys.clinical.entity.MedicalAttachment;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 医疗附件权限与元数据业务逻辑。 */
public interface MedicalAttachmentService {
public Map<String, Object> save(AuthenticatedUser user, long patientId, Long visitId, String attachmentType, String originalName, String storedName, String filePath, String contentType, long fileSize, String description);

  public List<Map<String, Object>> list(AuthenticatedUser user, Long requestedPatientId, Long visitId);

  public Map<String, Object> get(AuthenticatedUser user, long id);

  public Map<String, Object> delete(AuthenticatedUser user, long id);
}
