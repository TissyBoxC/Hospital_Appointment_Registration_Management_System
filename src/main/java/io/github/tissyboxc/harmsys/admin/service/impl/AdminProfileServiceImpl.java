package io.github.tissyboxc.harmsys.admin.service.impl;

import io.github.tissyboxc.harmsys.admin.service.AdminProfileService;

import io.github.tissyboxc.harmsys.admin.dto.AdminDoctorUpdateRequest;
import io.github.tissyboxc.harmsys.admin.dto.AdminPatientUpdateRequest;
import io.github.tissyboxc.harmsys.admin.mapper.AdminProfileMapper;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.operationlog.mapper.OperationLogMapper;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理员维护患者和医生资料的业务逻辑。 */
@Service
public class AdminProfileServiceImpl implements AdminProfileService {
  private final AdminProfileMapper mapper;
  private final OperationLogMapper operationLogMapper;

  public AdminProfileServiceImpl(
      AdminProfileMapper mapper, OperationLogMapper operationLogMapper) {
    this.mapper = mapper;
    this.operationLogMapper = operationLogMapper;
  }

  @Transactional(readOnly = true)
  @Override
  public Map<String, Object> patient(AuthenticatedUser operator, long id) {
    requireAdmin(operator);
    Map<String, Object> patient = mapper.selectPatient(id);
    if (patient == null) throw new UserRegistrationException(404, "患者资料不存在");
    return patient;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> updatePatient(
      AuthenticatedUser operator, long id, AdminPatientUpdateRequest request, String ipAddress) {
    requireAdmin(operator);
    if (mapper.updatePatient(id, request) != 1)
      throw new UserRegistrationException(404, "患者资料不存在");
    log(operator.user_id(), "ADMIN_UPDATE_PATIENT", "patient", id, "管理员修改患者资料", ipAddress);
    return mapper.selectPatientAfterUpdate(id);
  }

  @Transactional(readOnly = true)
  @Override
  public Map<String, Object> doctor(AuthenticatedUser operator, long id) {
    requireAdmin(operator);
    Map<String, Object> doctor = mapper.selectDoctor(id);
    if (doctor == null) throw new UserRegistrationException(404, "医生资料不存在");
    return doctor;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> updateDoctor(
      AuthenticatedUser operator, long id, AdminDoctorUpdateRequest request, String ipAddress) {
    requireAdmin(operator);
    if (mapper.updateDoctor(id, request) != 1)
      throw new UserRegistrationException(404, "医生资料不存在");
    log(operator.user_id(), "ADMIN_UPDATE_DOCTOR", "doctor", id, "管理员修改医生资料", ipAddress);
    return mapper.selectDoctorAfterUpdate(id);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void updateDoctorStatus(
      AuthenticatedUser operator, long id, int status, String ipAddress) {
    requireAdmin(operator);
    if (mapper.updateDoctorStatus(id, status) != 1)
      throw new UserRegistrationException(404, "医生资料不存在");
    log(operator.user_id(), "ADMIN_UPDATE_DOCTOR_STATUS", "doctor", id,
        "管理员修改医生执业状态", ipAddress);
  }

  private void requireAdmin(AuthenticatedUser user) {
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "只有管理员可以执行该操作");
  }

  private void log(
      long userId, String type, String targetType, long targetId, String description, String ip) {
    OperationLog log = new OperationLog();
    log.setUserId(userId);
    log.setOperationType(type);
    log.setTargetType(targetType);
    log.setTargetId(targetId);
    log.setDescription(description);
    log.setIpAddress(ip);
    operationLogMapper.insert(log);
  }
}

