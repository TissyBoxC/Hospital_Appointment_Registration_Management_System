package io.github.tissyboxc.harmsys.department.service;

import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.department.entity.Department;
import io.github.tissyboxc.harmsys.department.dto.DepartmentDoctorUpdateRequest;
import io.github.tissyboxc.harmsys.doctor.entity.DoctorSchedule;
import io.github.tissyboxc.harmsys.doctor.dto.ScheduleRequest;
import io.github.tissyboxc.harmsys.doctor.dto.ScheduleResult;
import io.github.tissyboxc.harmsys.doctor.dto.SlotRequest;
import io.github.tissyboxc.harmsys.doctor.dto.SlotResult;
import io.github.tissyboxc.harmsys.doctor.service.DoctorService;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.security.PermissionAuthorizationService;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 科室负责人范围内的医生、排班和患者业务逻辑。 */
public interface DepartmentManagerService {
public Map<String, Object> scope(AuthenticatedUser user);

  public List<Map<String, Object>> doctors(AuthenticatedUser user, String keyword);

  public Map<String, Object> doctor(AuthenticatedUser user, long doctorId);

  public Map<String, Object> updateDoctor(AuthenticatedUser user, long doctorId, DepartmentDoctorUpdateRequest body, String ipAddress);

  public List<ScheduleResult> schedules(AuthenticatedUser user, Long doctorId, LocalDate scheduleDate);

  public ScheduleResult schedule(AuthenticatedUser user, long scheduleId);

  public ScheduleResult createSchedule(AuthenticatedUser user, ScheduleRequest request, String ipAddress);

  public ScheduleResult updateSchedule(AuthenticatedUser user, long scheduleId, ScheduleRequest request, String ipAddress);

  public void deleteSchedule(AuthenticatedUser user, long scheduleId, String ipAddress);

  public List<SlotResult> slots(AuthenticatedUser user, long scheduleId);

  public SlotResult createSlot(AuthenticatedUser user, long scheduleId, SlotRequest request, String ipAddress);

  public void updateSlotStatus(AuthenticatedUser user, long slotId, int status, String ipAddress);

  public List<Map<String, Object>> patients(AuthenticatedUser user, String keyword);

  public Map<String, Object> patient(AuthenticatedUser user, long patientId);
}
