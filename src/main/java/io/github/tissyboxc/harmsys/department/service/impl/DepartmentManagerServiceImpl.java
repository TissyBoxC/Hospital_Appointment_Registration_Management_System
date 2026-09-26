package io.github.tissyboxc.harmsys.department.service.impl;

import io.github.tissyboxc.harmsys.department.service.DepartmentManagerService;

import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.department.entity.Department;
import io.github.tissyboxc.harmsys.department.dto.DepartmentDoctorUpdateRequest;
import io.github.tissyboxc.harmsys.department.mapper.DepartmentManagerMapper;
import io.github.tissyboxc.harmsys.doctor.entity.DoctorSchedule;
import io.github.tissyboxc.harmsys.doctor.dto.ScheduleRequest;
import io.github.tissyboxc.harmsys.doctor.dto.ScheduleResult;
import io.github.tissyboxc.harmsys.doctor.dto.SlotRequest;
import io.github.tissyboxc.harmsys.doctor.dto.SlotResult;
import io.github.tissyboxc.harmsys.doctor.mapper.DoctorScheduleMapper;
import io.github.tissyboxc.harmsys.doctor.service.DoctorService;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.operationlog.mapper.OperationLogMapper;
import io.github.tissyboxc.harmsys.security.PermissionAuthorizationService;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 科室负责人范围内的医生、排班和患者业务逻辑。 */
@Service
public class DepartmentManagerServiceImpl implements DepartmentManagerService {
  private static final String DEPARTMENT_MANAGE = "DEPARTMENT_MANAGE";

  private final DepartmentManagerMapper mapper;
  private final DoctorScheduleMapper scheduleMapper;
  private final OperationLogMapper operationLogMapper;
  private final DoctorService doctorService;
  private final PermissionAuthorizationService authorizationService;

  public DepartmentManagerServiceImpl(
      DepartmentManagerMapper mapper,
      DoctorScheduleMapper scheduleMapper,
      OperationLogMapper operationLogMapper,
      DoctorService doctorService,
      PermissionAuthorizationService authorizationService) {
    this.mapper = mapper;
    this.scheduleMapper = scheduleMapper;
    this.operationLogMapper = operationLogMapper;
    this.doctorService = doctorService;
    this.authorizationService = authorizationService;
  }

  @Transactional(readOnly = true)
  @Override
  public Map<String, Object> scope(AuthenticatedUser user) {
    List<Long> departments = managedDepartmentIds(user, DEPARTMENT_MANAGE);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("department_ids", departments);
    result.put("departments", mapper.selectDepartments(departments));
    return result;
  }

  @Transactional(readOnly = true)
  @Override
  public List<Map<String, Object>> doctors(AuthenticatedUser user, String keyword) {
    List<Long> departments = managedDepartmentIds(user, DEPARTMENT_MANAGE);
    return mapper.selectDoctors(
        departments, keyword == null ? null : keyword.trim());
  }

  @Transactional(readOnly = true)
  @Override
  public Map<String, Object> doctor(AuthenticatedUser user, long doctorId) {
    List<Long> departments = managedDepartmentIds(user, DEPARTMENT_MANAGE);
    Map<String, Object> doctor = mapper.selectDoctor(departments, doctorId);
    if (doctor == null) throw new UserRegistrationException(404, "医生不存在或不属于负责科室");
    return doctor;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> updateDoctor(
      AuthenticatedUser user,
      long doctorId,
      DepartmentDoctorUpdateRequest body,
      String ipAddress) {
    List<Long> departments = managedDepartmentIds(user, DEPARTMENT_MANAGE);
    assertDoctorInScope(departments, doctorId);
    if (body.status() < 0 || body.status() > 1)
      throw new UserRegistrationException(422, "医生状态只能是0或1");
    if (mapper.updateDoctor(doctorId, body) != 1)
      throw new UserRegistrationException(404, "医生不存在或不属于负责科室");
    log(
        user.user_id(), "DEPARTMENT_MANAGER_UPDATE_DOCTOR", "doctor", doctorId,
        "科室负责人修改医生资料", ipAddress);
    return doctor(user, doctorId);
  }

  @Transactional(readOnly = true)
  @Override
  public List<ScheduleResult> schedules(
      AuthenticatedUser user, Long doctorId, LocalDate scheduleDate) {
    List<Long> departments = managedDepartmentIds(user, DEPARTMENT_MANAGE);
    if (doctorId != null) assertDoctorInScope(departments, doctorId);
    return mapper.selectSchedules(departments, doctorId, scheduleDate).stream()
        .map(this::toScheduleResult)
        .toList();
  }

  @Transactional(readOnly = true)
  @Override
  public ScheduleResult schedule(AuthenticatedUser user, long scheduleId) {
    List<Long> departments = managedDepartmentIds(user, DEPARTMENT_MANAGE);
    Map<String, Object> schedule = mapper.selectSchedule(departments, scheduleId);
    if (schedule == null) throw new UserRegistrationException(404, "排班不存在或不属于负责科室");
    return toScheduleResult(schedule);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public ScheduleResult createSchedule(
      AuthenticatedUser user, ScheduleRequest request, String ipAddress) {
    List<Long> departments = managedDepartmentIds(user, DEPARTMENT_MANAGE);
    if (request.department_id() == null || !departments.contains(request.department_id()))
      throw new UserRegistrationException(403, "不能为其他科室创建排班");
    assertDoctorInScope(departments, request.doctor_id());
    validateSchedule(request);
    if (!doctorService.departmentMatchesDoctor(
        request.doctor_id(), request.department_id()))
      throw new UserRegistrationException(422, "排班科室与医生所属科室不一致");
    try {
      DoctorSchedule schedule = toScheduleEntity(request);
      schedule.setBookedCount(0);
      schedule.setStatus(0);
      scheduleMapper.insert(schedule);
      long id = schedule.getId();
      log(
          user.user_id(), "DEPARTMENT_MANAGER_CREATE_SCHEDULE", "doctor_schedule", id,
          "科室负责人创建排班", ipAddress);
      return schedule(user, id);
    } catch (DuplicateKeyException e) {
      throw new UserRegistrationException(409, "同一医生同一天同一时段已存在排班");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public ScheduleResult updateSchedule(
      AuthenticatedUser user, long scheduleId, ScheduleRequest request, String ipAddress) {
    List<Long> departments = managedDepartmentIds(user, DEPARTMENT_MANAGE);
    if (mapper.selectSchedule(departments, scheduleId) == null)
      throw new UserRegistrationException(404, "排班不存在或不属于负责科室");
    if (request.department_id() == null || !departments.contains(request.department_id()))
      throw new UserRegistrationException(403, "不能修改为其他科室的排班");
    assertDoctorInScope(departments, request.doctor_id());
    validateSchedule(request);
    if (!doctorService.departmentMatchesDoctor(
        request.doctor_id(), request.department_id()))
      throw new UserRegistrationException(422, "排班科室与医生所属科室不一致");
    DoctorSchedule schedule = toScheduleEntity(request);
    schedule.setId(scheduleId);
    if (scheduleMapper.updateWithoutBookings(schedule) != 1)
      throw new UserRegistrationException(409, "排班不存在，或已有预约不能修改");
    log(
        user.user_id(), "DEPARTMENT_MANAGER_UPDATE_SCHEDULE", "doctor_schedule", scheduleId,
        "科室负责人修改排班", ipAddress);
    return schedule(user, scheduleId);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void deleteSchedule(
      AuthenticatedUser user, long scheduleId, String ipAddress) {
    List<Long> departments = managedDepartmentIds(user, DEPARTMENT_MANAGE);
    if (mapper.selectSchedule(departments, scheduleId) == null)
      throw new UserRegistrationException(404, "排班不存在或不属于负责科室");
    if (scheduleMapper.deleteWithoutBookings(scheduleId) != 1)
      throw new UserRegistrationException(409, "排班不存在，或已有预约不能删除");
    log(
        user.user_id(), "DEPARTMENT_MANAGER_DELETE_SCHEDULE", "doctor_schedule", scheduleId,
        "科室负责人删除排班", ipAddress);
  }

  @Transactional(readOnly = true)
  @Override
  public List<SlotResult> slots(AuthenticatedUser user, long scheduleId) {
    requireSchedule(user, scheduleId);
    return doctorService.findSlots(scheduleId);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public SlotResult createSlot(
      AuthenticatedUser user, long scheduleId, SlotRequest request, String ipAddress) {
    requireSchedule(user, scheduleId);
    validateSlot(scheduleId, request);
    try {
      long id = doctorService.insertSlot(scheduleId, request);
      log(
          user.user_id(), "DEPARTMENT_MANAGER_CREATE_SLOT", "schedule_slot", id,
          "科室负责人创建排班时间段", ipAddress);
      return doctorService.findSlot(id).orElseThrow();
    } catch (DuplicateKeyException e) {
      throw new UserRegistrationException(409, "时间段序号或时间范围重复");
    }
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void updateSlotStatus(
      AuthenticatedUser user, long slotId, int status, String ipAddress) {
    if (status < 0 || status > 2)
      throw new UserRegistrationException(422, "时间段状态只能是0、1、2");
    SlotResult slot =
        doctorService
            .findSlot(slotId)
            .orElseThrow(() -> new UserRegistrationException(404, "时间段不存在"));
    requireSchedule(user, slot.schedule_id());
    doctorService.updateSlotStatus(slotId, status);
    log(
        user.user_id(), "DEPARTMENT_MANAGER_UPDATE_SLOT_STATUS", "schedule_slot", slotId,
        "科室负责人修改排班时间段状态", ipAddress);
  }

  @Transactional(readOnly = true)
  @Override
  public List<Map<String, Object>> patients(AuthenticatedUser user, String keyword) {
    List<Long> departments = managedDepartmentIds(user, DEPARTMENT_MANAGE);
    return mapper.selectPatients(
        departments, keyword == null ? null : keyword.trim());
  }

  @Transactional(readOnly = true)
  @Override
  public Map<String, Object> patient(AuthenticatedUser user, long patientId) {
    List<Long> departments = managedDepartmentIds(user, DEPARTMENT_MANAGE);
    Map<String, Object> patient = mapper.selectPatient(departments, patientId);
    if (patient == null) throw new UserRegistrationException(404, "患者不存在或未在负责科室就诊");
    Map<String, Object> result = new LinkedHashMap<>(patient);
    result.put("appointments", mapper.selectPatientAppointments(departments, patientId));
    result.put("visits", mapper.selectPatientVisits(departments, patientId));
    return result;
  }

  private List<Long> managedDepartmentIds(AuthenticatedUser user, String permission) {
    if (user.role_codes().stream().anyMatch("ADMIN"::equalsIgnoreCase))
      return mapper.selectAllDepartmentIds();
    authorizationService.requirePermissionForUser(user, permission);
    List<Long> departments = mapper.selectManagedDepartmentIds(user.user_id());
    if (departments.isEmpty() && user.department_id() != null)
      return List.of(user.department_id());
    if (departments.isEmpty()) throw new UserRegistrationException(403, "当前账号不是任何科室的负责人");
    return departments;
  }

  private void assertDoctorInScope(List<Long> departments, Long doctorId) {
    if (doctorId == null || mapper.countDoctorInDepartments(departments, doctorId) == 0)
      throw new UserRegistrationException(403, "医生不存在或不属于负责科室");
  }

  private void requireSchedule(AuthenticatedUser user, long scheduleId) {
    List<Long> departments = managedDepartmentIds(user, DEPARTMENT_MANAGE);
    if (mapper.selectSchedule(departments, scheduleId) == null)
      throw new UserRegistrationException(404, "排班不存在或不属于负责科室");
  }

  private DoctorSchedule toScheduleEntity(ScheduleRequest request) {
    DoctorSchedule schedule = new DoctorSchedule();
    schedule.setDoctorId(request.doctor_id());
    schedule.setDepartmentId(request.department_id());
    schedule.setScheduleDate(request.schedule_date());
    schedule.setPeriod(request.period());
    schedule.setStartTime(request.start_time());
    schedule.setEndTime(request.end_time());
    schedule.setTotalCount(request.total_count());
    schedule.setFee(request.fee());
    schedule.setRemark(request.remark());
    return schedule;
  }

  private ScheduleResult toScheduleResult(Map<String, Object> row) {
    return new ScheduleResult(
        number(row, "id"),
        number(row, "doctor_id"),
        number(row, "department_id"),
        (LocalDate) row.get("schedule_date"),
        integer(row, "period"),
        (java.time.LocalTime) row.get("start_time"),
        (java.time.LocalTime) row.get("end_time"),
        integer(row, "total_count"),
        integer(row, "booked_count"),
        (java.math.BigDecimal) row.get("fee"),
        integer(row, "status"),
        row.get("remark") == null ? null : String.valueOf(row.get("remark")));
  }

  private long number(Map<String, Object> row, String key) {
    return ((Number) row.get(key)).longValue();
  }

  private int integer(Map<String, Object> row, String key) {
    return ((Number) row.get(key)).intValue();
  }

  private void log(
      long userId,
      String operation,
      String targetType,
      long targetId,
      String description,
      String ipAddress) {
    OperationLog log = new OperationLog();
    log.setUserId(userId);
    log.setOperationType(operation);
    log.setTargetType(targetType);
    log.setTargetId(targetId);
    log.setDescription(description);
    log.setIpAddress(ipAddress);
    operationLogMapper.insert(log);
  }

  private void validateSchedule(ScheduleRequest request) {
    if (request.doctor_id() == null) throw new UserRegistrationException(422, "必须指定医生");
    if (request.schedule_date().isBefore(LocalDate.now()))
      throw new UserRegistrationException(422, "排班日期不能早于当前日期");
    if (!request.start_time().isBefore(request.end_time()))
      throw new UserRegistrationException(422, "排班开始时间必须早于结束时间");
    if (!doctorService.doctorEnabled(request.doctor_id()))
      throw new UserRegistrationException(422, "医生不存在或已停诊");
  }

  private void validateSlot(long scheduleId, SlotRequest request) {
    ScheduleResult schedule =
        doctorService
            .findSchedule(scheduleId)
            .orElseThrow(() -> new UserRegistrationException(404, "排班不存在"));
    if (!request.start_time().isBefore(request.end_time())
        || request.start_time().isBefore(schedule.start_time())
        || request.end_time().isAfter(schedule.end_time()))
      throw new UserRegistrationException(422, "时间段必须位于排班时间范围内");
    if (doctorService.slotNumberExists(scheduleId, request.slot_no())
        || doctorService.slotOverlaps(
            scheduleId, request.start_time(), request.end_time()))
      throw new UserRegistrationException(409, "时间段序号或时间范围重复");
  }
}
