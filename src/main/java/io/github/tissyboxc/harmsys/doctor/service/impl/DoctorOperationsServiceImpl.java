package io.github.tissyboxc.harmsys.doctor.service.impl;

import io.github.tissyboxc.harmsys.doctor.service.DoctorOperationsService;

import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.doctor.mapper.DoctorOperationsMapper;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 医生工作台患者与就诊历史业务逻辑。 */
@Service
public class DoctorOperationsServiceImpl implements DoctorOperationsService {
  private final DoctorOperationsMapper mapper;

  public DoctorOperationsServiceImpl(DoctorOperationsMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  @Override
  public List<Map<String, Object>> patients(AuthenticatedUser user, String keyword) {
    return mapper.selectPatients(requireDoctor(user).doctor_id(), keyword);
  }

  @Transactional(readOnly = true)
  @Override
  public Map<String, Object> patient(AuthenticatedUser user, long patientId) {
    return requirePatient(requireDoctor(user).doctor_id(), patientId);
  }

  @Transactional(readOnly = true)
  @Override
  public Map<String, Object> history(AuthenticatedUser user, long patientId) {
    long doctorId = requireDoctor(user).doctor_id();
    requirePatient(doctorId, patientId);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("appointments", mapper.selectAppointments(patientId, doctorId));
    result.put("visits", mapper.selectVisits(patientId, doctorId));
    result.put("diagnoses", mapper.selectDiagnoses(patientId, doctorId));
    result.put("prescriptions", mapper.selectPrescriptions(patientId, doctorId));
    return result;
  }

  @Override
  public Map<String, Object> rejectDirectSlotUpdate(AuthenticatedUser user) {
    requireDoctor(user);
    throw new UserRegistrationException(409, "医生不能直接修改正式排班时间段，请提交排班修改申请");
  }

  @Override
  public void rejectDirectSlotDelete(AuthenticatedUser user) {
    requireDoctor(user);
    throw new UserRegistrationException(409, "医生不能直接删除正式排班时间段，请提交排班修改或删除申请");
  }

  private Map<String, Object> requirePatient(long doctorId, long patientId) {
    Map<String, Object> patient = mapper.selectPatient(patientId, doctorId);
    if (patient == null) throw new UserRegistrationException(404, "患者不存在或未挂过当前医生的号");
    return patient;
  }

  private AuthenticatedUser requireDoctor(AuthenticatedUser user) {
    if (user.doctor_id() == null
        || user.role_codes().stream().noneMatch("DOCTOR"::equalsIgnoreCase))
      throw new UserRegistrationException(403, "当前账号不是医生");
    return user;
  }
}
