package io.github.tissyboxc.harmsys.patient.service;

import io.github.tissyboxc.harmsys.patient.dto.PatientProfileUpdateRequest;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.Map;

/** 患者个人资料业务。 */
public interface PatientService {

  Map<String, Object> getProfile(AuthenticatedUser user);

  Map<String, Object> updateProfile(
      AuthenticatedUser user, PatientProfileUpdateRequest body, String ipAddress);
}
