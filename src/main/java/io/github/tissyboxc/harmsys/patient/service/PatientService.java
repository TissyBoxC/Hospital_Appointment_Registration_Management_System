package io.github.tissyboxc.harmsys.patient.service;

import io.github.tissyboxc.harmsys.patient.dto.PatientProfileUpdateRequest;
import io.github.tissyboxc.harmsys.patient.repository.PatientRepository;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 患者个人资料业务逻辑。 */
@Service
public class PatientService {
  private final PatientRepository repository;

  public PatientService(PatientRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getProfile(AuthenticatedUser user) {
    long patientId = requirePatient(user);
    return repository.findProfile(patientId);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> updateProfile(
      AuthenticatedUser user, PatientProfileUpdateRequest body, String ipAddress) {
    long patientId = requirePatient(user);
    if (repository.updateProfile(patientId, body) != 1)
      throw new UserRegistrationException(404, "患者资料不存在");
    repository.insertOperationLog(user.user_id(), patientId, "患者修改个人资料", ipAddress);
    return repository.findProfile(patientId);
  }

  private long requirePatient(AuthenticatedUser user) {
    if (user.patient_id() == null
        || user.role_codes().stream().noneMatch("PATIENT"::equalsIgnoreCase))
      throw new UserRegistrationException(403, "当前账号不是患者");
    return user.patient_id();
  }
}

