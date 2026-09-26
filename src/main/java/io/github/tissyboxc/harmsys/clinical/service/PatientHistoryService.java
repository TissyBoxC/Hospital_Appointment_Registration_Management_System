package io.github.tissyboxc.harmsys.clinical.service;

import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.List;
import java.util.Map;

/** 患者医疗历史业务逻辑。 */
public interface PatientHistoryService {
public List<Map<String, Object>> prescriptions(AuthenticatedUser user);

  public List<Map<String, Object>> items(AuthenticatedUser user, long prescriptionId);

  public List<Map<String, Object>> diagnoses(AuthenticatedUser user, Long visitId);
}
