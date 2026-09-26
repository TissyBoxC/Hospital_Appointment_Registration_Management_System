package io.github.tissyboxc.harmsys.patient.service.impl;

import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.operationlog.mapper.OperationLogMapper;
import io.github.tissyboxc.harmsys.patient.dto.PatientProfileUpdateRequest;
import io.github.tissyboxc.harmsys.patient.entity.Patient;
import io.github.tissyboxc.harmsys.patient.mapper.PatientMapper;
import io.github.tissyboxc.harmsys.patient.service.PatientService;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 患者个人资料业务实现。 */
@Service
public class PatientServiceImpl implements PatientService {
  private final PatientMapper patientMapper;
  private final OperationLogMapper operationLogMapper;

  public PatientServiceImpl(PatientMapper patientMapper, OperationLogMapper operationLogMapper) {
    this.patientMapper = patientMapper;
    this.operationLogMapper = operationLogMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Object> getProfile(AuthenticatedUser user) {
    long patientId = requirePatient(user);
    Map<String, Object> profile = patientMapper.selectProfile(patientId);
    if (profile == null) throw new UserRegistrationException(404, "患者资料不存在");
    return profile;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> updateProfile(
      AuthenticatedUser user, PatientProfileUpdateRequest body, String ipAddress) {
    long patientId = requirePatient(user);
    Patient patient = new Patient();
    patient.setId(patientId);
    patient.setRealName(body.real_name());
    patient.setPhone(body.phone());
    patient.setAddress(body.address());
    patient.setEmergencyContact(body.emergency_contact());
    patient.setEmergencyPhone(body.emergency_phone());
    if (patientMapper.updateById(patient) != 1)
      throw new UserRegistrationException(404, "患者资料不存在");

    OperationLog log = new OperationLog();
    log.setUserId(user.user_id());
    log.setOperationType("UPDATE_PATIENT_PROFILE");
    log.setTargetType("patient");
    log.setTargetId(patientId);
    log.setDescription("患者修改个人资料");
    log.setIpAddress(ipAddress);
    operationLogMapper.insert(log);
    return getProfile(user);
  }

  private long requirePatient(AuthenticatedUser user) {
    if (user.patient_id() == null
        || user.role_codes().stream().noneMatch("PATIENT"::equalsIgnoreCase))
      throw new UserRegistrationException(403, "当前账号不是患者");
    return user.patient_id();
  }
}
