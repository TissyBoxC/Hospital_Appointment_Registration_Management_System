package io.github.tissyboxc.harmsys.clinical.service.impl;

import io.github.tissyboxc.harmsys.clinical.service.PatientHistoryService;

import io.github.tissyboxc.harmsys.clinical.mapper.PatientHistoryMapper;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 患者医疗历史业务逻辑。 */
@Service
public class PatientHistoryServiceImpl implements PatientHistoryService {
  private final PatientHistoryMapper mapper;

  public PatientHistoryServiceImpl(PatientHistoryMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  @Override
  public List<Map<String, Object>> prescriptions(AuthenticatedUser user) {
    return mapper.selectPrescriptions(requirePatient(user));
  }

  @Transactional(readOnly = true)
  @Override
  public List<Map<String, Object>> items(AuthenticatedUser user, long prescriptionId) {
    long patientId = requirePatient(user);
    if (mapper.countOwnedPrescription(prescriptionId, patientId) == 0)
      throw new UserRegistrationException(404, "处方不存在");
    return mapper.selectPrescriptionItems(prescriptionId);
  }

  @Transactional(readOnly = true)
  @Override
  public List<Map<String, Object>> diagnoses(AuthenticatedUser user, Long visitId) {
    long patientId = requirePatient(user);
    if (visitId == null) return mapper.selectDiagnoses(patientId);
    if (mapper.countOwnedVisit(visitId, patientId) == 0)
      throw new UserRegistrationException(404, "就诊记录不存在");
    return mapper.selectDiagnosesByVisit(visitId);
  }

  private long requirePatient(AuthenticatedUser user) {
    if (user.patient_id() == null
        || user.role_codes().stream().noneMatch("PATIENT"::equalsIgnoreCase))
      throw new UserRegistrationException(403, "当前账号不是患者");
    return user.patient_id();
  }
}


