package io.github.tissyboxc.harmsys.admin.service;

import io.github.tissyboxc.harmsys.admin.dto.AdminDoctorUpdateRequest;
import io.github.tissyboxc.harmsys.admin.dto.AdminPatientUpdateRequest;
import io.github.tissyboxc.harmsys.admin.repository.AdminProfileRepository;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理员维护患者和医生资料的业务逻辑。 */
@Service
public class AdminProfileService {
  private final AdminProfileRepository repository;

  public AdminProfileService(AdminProfileRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> patient(AuthenticatedUser operator, long id) {
    requireAdmin(operator);
    try {
      return repository.findPatient(id);
    } catch (EmptyResultDataAccessException e) {
      throw new UserRegistrationException(404, "患者资料不存在");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> updatePatient(
      AuthenticatedUser operator, long id, AdminPatientUpdateRequest request, String ipAddress) {
    requireAdmin(operator);
    if (repository.updatePatient(id, request) != 1)
      throw new UserRegistrationException(404, "患者资料不存在");
    repository.insertPatientOperationLog(
        operator.user_id(), "ADMIN_UPDATE_PATIENT", id, "管理员修改患者资料", ipAddress);
    return repository.findPatientAfterUpdate(id);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> doctor(AuthenticatedUser operator, long id) {
    requireAdmin(operator);
    try {
      return repository.findDoctor(id);
    } catch (EmptyResultDataAccessException e) {
      throw new UserRegistrationException(404, "医生资料不存在");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> updateDoctor(
      AuthenticatedUser operator, long id, AdminDoctorUpdateRequest request, String ipAddress) {
    requireAdmin(operator);
    if (repository.updateDoctor(id, request) != 1)
      throw new UserRegistrationException(404, "医生资料不存在");
    repository.insertDoctorOperationLog(
        operator.user_id(), "ADMIN_UPDATE_DOCTOR", id, "管理员修改医生资料", ipAddress);
    return repository.findDoctorAfterUpdate(id);
  }

  @Transactional(rollbackFor = Exception.class)
  public void updateDoctorStatus(
      AuthenticatedUser operator, long id, int status, String ipAddress) {
    requireAdmin(operator);
    if (repository.updateDoctorStatus(id, status) != 1)
      throw new UserRegistrationException(404, "医生资料不存在");
    repository.insertDoctorOperationLog(
        operator.user_id(), "ADMIN_UPDATE_DOCTOR_STATUS", id, "管理员修改医生执业状态", ipAddress);
  }

  private void requireAdmin(AuthenticatedUser user) {
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "只有管理员可以执行该操作");
  }
}

