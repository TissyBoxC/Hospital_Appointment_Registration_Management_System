package io.github.tissyboxc.harmsys.admin.service;

import io.github.tissyboxc.harmsys.admin.dto.AdminDoctorUpdateRequest;
import io.github.tissyboxc.harmsys.admin.dto.AdminPatientUpdateRequest;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.Map;

/** 管理员维护患者和医生资料的业务逻辑。 */
public interface AdminProfileService {
public Map<String, Object> patient(AuthenticatedUser operator, long id);

  public Map<String, Object> updatePatient(AuthenticatedUser operator, long id, AdminPatientUpdateRequest request, String ipAddress);

  public Map<String, Object> doctor(AuthenticatedUser operator, long id);

  public Map<String, Object> updateDoctor(AuthenticatedUser operator, long id, AdminDoctorUpdateRequest request, String ipAddress);

  public void updateDoctorStatus(AuthenticatedUser operator, long id, int status, String ipAddress);
}
