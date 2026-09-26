package io.github.tissyboxc.harmsys.doctor.service;

import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 医生工作台患者与就诊历史业务逻辑。 */
public interface DoctorOperationsService {
public List<Map<String, Object>> patients(AuthenticatedUser user, String keyword);

  public Map<String, Object> patient(AuthenticatedUser user, long patientId);

  public Map<String, Object> history(AuthenticatedUser user, long patientId);

  public Map<String, Object> rejectDirectSlotUpdate(AuthenticatedUser user);

  public void rejectDirectSlotDelete(AuthenticatedUser user);
}
