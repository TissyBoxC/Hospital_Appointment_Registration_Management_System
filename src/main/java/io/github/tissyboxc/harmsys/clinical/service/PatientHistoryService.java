package io.github.tissyboxc.harmsys.clinical.service;

import io.github.tissyboxc.harmsys.clinical.repository.PatientHistoryRepository;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 患者医疗历史业务逻辑。 */
@Service
public class PatientHistoryService {
  private final PatientHistoryRepository repository;

  public PatientHistoryService(PatientHistoryRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> prescriptions(AuthenticatedUser user) {
    return repository.prescriptions(requirePatient(user));
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> items(AuthenticatedUser user, long prescriptionId) {
    long patientId = requirePatient(user);
    if (!repository.ownsPrescription(prescriptionId, patientId))
      throw new UserRegistrationException(404, "处方不存在");
    return repository.prescriptionItems(prescriptionId);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> diagnoses(AuthenticatedUser user, Long visitId) {
    long patientId = requirePatient(user);
    if (visitId == null) return repository.diagnoses(patientId);
    if (!repository.ownsVisit(visitId, patientId))
      throw new UserRegistrationException(404, "就诊记录不存在");
    return repository.diagnosesByVisit(visitId);
  }

  private long requirePatient(AuthenticatedUser user) {
    if (user.patient_id() == null
        || user.role_codes().stream().noneMatch("PATIENT"::equalsIgnoreCase))
      throw new UserRegistrationException(403, "当前账号不是患者");
    return user.patient_id();
  }
}


