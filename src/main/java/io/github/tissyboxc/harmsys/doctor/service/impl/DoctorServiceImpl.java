package io.github.tissyboxc.harmsys.doctor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.doctor.dto.DoctorProfileResult;
import io.github.tissyboxc.harmsys.doctor.dto.DoctorProfileUpdateRequest;
import io.github.tissyboxc.harmsys.doctor.dto.ScheduleRequest;
import io.github.tissyboxc.harmsys.doctor.dto.ScheduleResult;
import io.github.tissyboxc.harmsys.doctor.dto.SlotRequest;
import io.github.tissyboxc.harmsys.doctor.dto.SlotResult;
import io.github.tissyboxc.harmsys.doctor.entity.Doctor;
import io.github.tissyboxc.harmsys.doctor.entity.DoctorSchedule;
import io.github.tissyboxc.harmsys.doctor.entity.ScheduleSlot;
import io.github.tissyboxc.harmsys.doctor.mapper.DoctorMapper;
import io.github.tissyboxc.harmsys.doctor.mapper.DoctorScheduleMapper;
import io.github.tissyboxc.harmsys.doctor.mapper.ScheduleSlotMapper;
import io.github.tissyboxc.harmsys.doctor.service.DoctorService;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.operationlog.mapper.OperationLogMapper;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 医生资料、排班和时间段维护业务实现。 */
@Service
public class DoctorServiceImpl implements DoctorService {
  private final DoctorMapper doctorMapper;
  private final DoctorScheduleMapper scheduleMapper;
  private final ScheduleSlotMapper slotMapper;
  private final OperationLogMapper operationLogMapper;

  public DoctorServiceImpl(
      DoctorMapper doctorMapper,
      DoctorScheduleMapper scheduleMapper,
      ScheduleSlotMapper slotMapper,
      OperationLogMapper operationLogMapper) {
    this.doctorMapper = doctorMapper;
    this.scheduleMapper = scheduleMapper;
    this.slotMapper = slotMapper;
    this.operationLogMapper = operationLogMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public DoctorProfileResult profile(HttpServletRequest request) {
    return findProfile(currentDoctor(request).doctor_id())
        .orElseThrow(() -> new UserRegistrationException(404, "医生资料不存在"));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public DoctorProfileResult updateProfile(
      DoctorProfileUpdateRequest update, HttpServletRequest request) {
    AuthenticatedUser user = currentDoctor(request);
    Doctor doctor = new Doctor();
    doctor.setId(user.doctor_id());
    doctor.setRealName(update.real_name());
    doctor.setTitle(update.title());
    doctor.setSpecialty(update.specialty());
    doctor.setIntroduction(update.introduction());
    doctor.setAvatarUrl(update.avatar_url());
    doctor.setConsultationFee(update.consultation_fee());
    if (doctorMapper.updateById(doctor) != 1)
      throw new UserRegistrationException(404, "医生资料不存在");
    writeLog(
        user.user_id(), "UPDATE_DOCTOR_PROFILE", "doctor", user.doctor_id(),
        "医生修改个人资料", request.getRemoteAddr());
    return profile(request);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ScheduleResult> schedules(HttpServletRequest request) {
    return findSchedules(currentDoctor(request).doctor_id());
  }

  @Override
  @Transactional(readOnly = true)
  public List<ScheduleResult> allSchedules(HttpServletRequest request) {
    requireAdmin(request);
    return scheduleMapper
        .selectList(
            new LambdaQueryWrapper<DoctorSchedule>()
                .orderByAsc(DoctorSchedule::getScheduleDate)
                .orderByAsc(DoctorSchedule::getStartTime))
        .stream()
        .map(this::toScheduleResult)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public ScheduleResult schedule(long scheduleId) {
    return findSchedule(scheduleId)
        .orElseThrow(() -> new UserRegistrationException(404, "排班不存在"));
  }

  @Override
  @Transactional(readOnly = true)
  public ScheduleResult adminSchedule(long scheduleId, HttpServletRequest request) {
    requireAdmin(request);
    return schedule(scheduleId);
  }

  @Override
  @Transactional(readOnly = true)
  public ScheduleResult scheduleForDoctor(long scheduleId, HttpServletRequest request) {
    AuthenticatedUser user = currentDoctor(request);
    if (!scheduleBelongsTo(scheduleId, user.doctor_id()))
      throw new UserRegistrationException(404, "排班不存在或不属于当前医生");
    return schedule(scheduleId);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ScheduleResult createOwnSchedule(ScheduleRequest request, HttpServletRequest httpRequest) {
    AuthenticatedUser user = currentDoctor(httpRequest);
    ScheduleRequest actual = withDoctor(request, user.doctor_id());
    validateSchedule(actual);
    if (!departmentMatchesDoctor(user.doctor_id(), actual.department_id()))
      throw new UserRegistrationException(422, "排班科室必须是医生所属科室");
    try {
      DoctorSchedule schedule = toEntity(actual);
      schedule.setBookedCount(0);
      schedule.setStatus(0);
      scheduleMapper.insert(schedule);
      writeLog(
          user.user_id(), "CREATE_OWN_SCHEDULE", "doctor_schedule", schedule.getId(),
          "医生创建本人排班", httpRequest.getRemoteAddr());
      return schedule(schedule.getId());
    } catch (DuplicateKeyException exception) {
      throw new UserRegistrationException(409, "同一医生同一天同一时段已存在排班");
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ScheduleResult createAdminSchedule(
      ScheduleRequest request, HttpServletRequest httpRequest) {
    AuthenticatedUser user = requireAdmin(httpRequest);
    validateSchedule(request);
    if (!departmentMatchesDoctor(request.doctor_id(), request.department_id()))
      throw new UserRegistrationException(422, "排班科室与医生所属科室不一致");
    try {
      DoctorSchedule schedule = toEntity(request);
      schedule.setBookedCount(0);
      schedule.setStatus(0);
      scheduleMapper.insert(schedule);
      writeLog(
          user.user_id(), "ADMIN_CREATE_SCHEDULE", "doctor_schedule", schedule.getId(),
          "管理员创建排班", httpRequest.getRemoteAddr());
      return schedule(schedule.getId());
    } catch (DuplicateKeyException exception) {
      throw new UserRegistrationException(409, "同一医生同一天同一时段已存在排班");
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ScheduleResult updateOwnSchedule(
      long scheduleId, ScheduleRequest request, HttpServletRequest httpRequest) {
    AuthenticatedUser user = currentDoctor(httpRequest);
    if (!scheduleBelongsTo(scheduleId, user.doctor_id()))
      throw new UserRegistrationException(404, "排班不存在或不属于当前医生");
    ScheduleRequest actual = withDoctor(request, user.doctor_id());
    validateSchedule(actual);
    if (!departmentMatchesDoctor(user.doctor_id(), actual.department_id()))
      throw new UserRegistrationException(422, "排班科室必须是医生所属科室");
    updateWithoutBookings(scheduleId, actual);
    writeLog(
        user.user_id(), "UPDATE_OWN_SCHEDULE", "doctor_schedule", scheduleId,
        "医生修改本人排班", httpRequest.getRemoteAddr());
    return schedule(scheduleId);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ScheduleResult updateAdminSchedule(
      long scheduleId, ScheduleRequest request, HttpServletRequest httpRequest) {
    AuthenticatedUser user = requireAdmin(httpRequest);
    if (findSchedule(scheduleId).isEmpty())
      throw new UserRegistrationException(404, "排班不存在");
    validateSchedule(request);
    if (!departmentMatchesDoctor(request.doctor_id(), request.department_id()))
      throw new UserRegistrationException(422, "排班科室与医生所属科室不一致");
    updateWithoutBookings(scheduleId, request);
    writeLog(
        user.user_id(), "ADMIN_UPDATE_SCHEDULE", "doctor_schedule", scheduleId,
        "管理员修改排班", httpRequest.getRemoteAddr());
    return schedule(scheduleId);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteOwnSchedule(long scheduleId, HttpServletRequest request) {
    AuthenticatedUser user = currentDoctor(request);
    if (!scheduleBelongsTo(scheduleId, user.doctor_id()))
      throw new UserRegistrationException(404, "排班不存在或不属于当前医生");
    deleteWithoutBookings(scheduleId);
    writeLog(
        user.user_id(), "DELETE_OWN_SCHEDULE", "doctor_schedule", scheduleId,
        "医生删除本人排班", request.getRemoteAddr());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteAdminSchedule(long scheduleId, HttpServletRequest request) {
    AuthenticatedUser user = requireAdmin(request);
    if (findSchedule(scheduleId).isEmpty())
      throw new UserRegistrationException(404, "排班不存在");
    deleteWithoutBookings(scheduleId);
    writeLog(
        user.user_id(), "ADMIN_DELETE_SCHEDULE", "doctor_schedule", scheduleId,
        "管理员删除排班", request.getRemoteAddr());
  }

  @Override
  @Transactional(readOnly = true)
  public List<SlotResult> adminSlots(long scheduleId, HttpServletRequest request) {
    requireAdmin(request);
    if (findSchedule(scheduleId).isEmpty())
      throw new UserRegistrationException(404, "排班不存在");
    return findSlots(scheduleId);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public SlotResult createAdminSlot(
      long scheduleId, SlotRequest slot, HttpServletRequest request) {
    AuthenticatedUser user = requireAdmin(request);
    if (findSchedule(scheduleId).isEmpty())
      throw new UserRegistrationException(404, "排班不存在");
    validateSlot(scheduleId, slot);
    try {
      long id = insertSlot(scheduleId, slot);
      writeLog(
          user.user_id(), "ADMIN_CREATE_SLOT", "schedule_slot", id,
          "管理员创建排班时间段", request.getRemoteAddr());
      return findSlot(id).orElseThrow();
    } catch (DuplicateKeyException exception) {
      throw new UserRegistrationException(409, "时间段序号或时间范围重复");
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void adminUpdateSlotStatus(long slotId, int status, HttpServletRequest request) {
    AuthenticatedUser user = requireAdmin(request);
    if (status < 0 || status > 2)
      throw new UserRegistrationException(422, "时间段状态只能是0、1、2");
    updateSlotStatus(slotId, status);
    writeLog(
        user.user_id(), "ADMIN_UPDATE_SLOT_STATUS", "schedule_slot", slotId,
        "管理员修改时间段状态", request.getRemoteAddr());
  }

  @Override
  @Transactional(readOnly = true)
  public List<SlotResult> slots(long scheduleId, HttpServletRequest request) {
    AuthenticatedUser user = currentDoctor(request);
    if (!scheduleBelongsTo(scheduleId, user.doctor_id()))
      throw new UserRegistrationException(404, "排班不存在或不属于当前医生");
    return findSlots(scheduleId);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public SlotResult createOwnSlot(
      long scheduleId, SlotRequest slot, HttpServletRequest request) {
    AuthenticatedUser user = currentDoctor(request);
    if (!scheduleBelongsTo(scheduleId, user.doctor_id()))
      throw new UserRegistrationException(404, "排班不存在或不属于当前医生");
    validateSlot(scheduleId, slot);
    try {
      long id = insertSlot(scheduleId, slot);
      writeLog(
          user.user_id(), "CREATE_OWN_SLOT", "schedule_slot", id,
          "医生创建排班时间段", request.getRemoteAddr());
      return findSlot(id).orElseThrow();
    } catch (DuplicateKeyException exception) {
      throw new UserRegistrationException(409, "时间段序号或时间范围重复");
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void updateSlotStatus(long slotId, int status, HttpServletRequest request) {
    AuthenticatedUser user = currentDoctor(request);
    SlotResult slot =
        findSlot(slotId)
            .orElseThrow(() -> new UserRegistrationException(404, "时间段不存在"));
    if (!scheduleBelongsTo(slot.schedule_id(), user.doctor_id()))
      throw new UserRegistrationException(403, "不能管理其他医生的时间段");
    if (status < 0 || status > 2)
      throw new UserRegistrationException(422, "时间段状态只能是0、1、2");
    updateSlotStatus(slotId, status);
    writeLog(
        user.user_id(), "UPDATE_SLOT_STATUS", "schedule_slot", slotId,
        "医生修改时间段状态", request.getRemoteAddr());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<DoctorProfileResult> findProfile(long doctorId) {
    return Optional.ofNullable(doctorMapper.selectProfile(doctorId));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Long> findDoctorDepartment(long doctorId) {
    Doctor doctor = doctorMapper.selectById(doctorId);
    return doctor == null ? Optional.empty() : Optional.of(doctor.getDepartmentId());
  }

  @Override
  @Transactional(readOnly = true)
  public boolean doctorEnabled(long doctorId) {
    return doctorMapper.selectCount(
            new LambdaQueryWrapper<Doctor>()
                .eq(Doctor::getId, doctorId)
                .eq(Doctor::getStatus, 1))
        > 0;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean departmentMatchesDoctor(long doctorId, long departmentId) {
    return doctorMapper.selectCount(
            new LambdaQueryWrapper<Doctor>()
                .eq(Doctor::getId, doctorId)
                .eq(Doctor::getDepartmentId, departmentId)
                .eq(Doctor::getStatus, 1))
        > 0;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean scheduleBelongsTo(long scheduleId, long doctorId) {
    return scheduleMapper.selectCount(
            new LambdaQueryWrapper<DoctorSchedule>()
                .eq(DoctorSchedule::getId, scheduleId)
                .eq(DoctorSchedule::getDoctorId, doctorId))
        > 0;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ScheduleResult> findSchedule(long id) {
    DoctorSchedule schedule = scheduleMapper.selectById(id);
    return schedule == null ? Optional.empty() : Optional.of(toScheduleResult(schedule));
  }

  @Override
  @Transactional(readOnly = true)
  public List<SlotResult> findSlots(long scheduleId) {
    return slotMapper
        .selectList(
            new LambdaQueryWrapper<ScheduleSlot>()
                .eq(ScheduleSlot::getScheduleId, scheduleId)
                .orderByAsc(ScheduleSlot::getSlotNo))
        .stream()
        .map(this::toSlotResult)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SlotResult> findSlot(long slotId) {
    ScheduleSlot slot = slotMapper.selectById(slotId);
    return slot == null ? Optional.empty() : Optional.of(toSlotResult(slot));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public long insertSlot(long scheduleId, SlotRequest request) {
    ScheduleSlot slot = new ScheduleSlot();
    slot.setScheduleId(scheduleId);
    slot.setSlotNo(request.slot_no());
    slot.setStartTime(request.start_time());
    slot.setEndTime(request.end_time());
    slot.setStatus(0);
    slotMapper.insert(slot);
    return slot.getId();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void updateSlotStatus(long slotId, int status) {
    ScheduleSlot slot = new ScheduleSlot();
    slot.setId(slotId);
    slot.setStatus(status);
    if (slotMapper.updateById(slot) != 1)
      throw new UserRegistrationException(404, "时间段不存在");
  }

  @Override
  @Transactional(readOnly = true)
  public boolean slotOverlaps(
      long scheduleId, java.time.LocalTime startTime, java.time.LocalTime endTime) {
    return slotMapper.countOverlaps(scheduleId, startTime, endTime) > 0;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean slotNumberExists(long scheduleId, int slotNo) {
    return slotMapper.selectCount(
            new LambdaQueryWrapper<ScheduleSlot>()
                .eq(ScheduleSlot::getScheduleId, scheduleId)
                .eq(ScheduleSlot::getSlotNo, slotNo))
        > 0;
  }

  @Override
  public void writeLog(
      long userId,
      String operationType,
      String targetType,
      Long targetId,
      String description,
      String ipAddress) {
    OperationLog log = new OperationLog();
    log.setUserId(userId);
    log.setOperationType(operationType);
    log.setTargetType(targetType);
    log.setTargetId(targetId);
    log.setDescription(description);
    log.setIpAddress(ipAddress);
    operationLogMapper.insert(log);
  }

  private List<ScheduleResult> findSchedules(long doctorId) {
    return scheduleMapper
        .selectList(
            new LambdaQueryWrapper<DoctorSchedule>()
                .eq(DoctorSchedule::getDoctorId, doctorId)
                .orderByAsc(DoctorSchedule::getScheduleDate)
                .orderByAsc(DoctorSchedule::getStartTime))
        .stream()
        .map(this::toScheduleResult)
        .toList();
  }

  private void updateWithoutBookings(long scheduleId, ScheduleRequest request) {
    DoctorSchedule schedule = toEntity(request);
    schedule.setId(scheduleId);
    if (scheduleMapper.updateWithoutBookings(schedule) != 1)
      throw new UserRegistrationException(409, "排班不存在，或已有预约不能修改");
  }

  private void deleteWithoutBookings(long scheduleId) {
    if (scheduleMapper.deleteWithoutBookings(scheduleId) != 1)
      throw new UserRegistrationException(409, "排班不存在，或已有预约不能删除");
  }

  private void validateSchedule(ScheduleRequest request) {
    if (request.doctor_id() == null)
      throw new UserRegistrationException(422, "必须指定医生");
    if (request.schedule_date().isBefore(LocalDate.now()))
      throw new UserRegistrationException(422, "排班日期不能早于当前日期");
    if (!request.start_time().isBefore(request.end_time()))
      throw new UserRegistrationException(422, "排班开始时间必须早于结束时间");
    if (!doctorEnabled(request.doctor_id()))
      throw new UserRegistrationException(422, "医生不存在或已停诊");
  }

  private void validateSlot(long scheduleId, SlotRequest request) {
    ScheduleResult schedule =
        findSchedule(scheduleId)
            .orElseThrow(() -> new UserRegistrationException(404, "排班不存在"));
    if (!request.start_time().isBefore(request.end_time())
        || request.start_time().isBefore(schedule.start_time())
        || request.end_time().isAfter(schedule.end_time()))
      throw new UserRegistrationException(422, "时间段必须位于排班时间范围内");
    if (slotNumberExists(scheduleId, request.slot_no())
        || slotOverlaps(scheduleId, request.start_time(), request.end_time()))
      throw new UserRegistrationException(409, "时间段序号或时间范围重复");
  }

  private ScheduleRequest withDoctor(ScheduleRequest request, long doctorId) {
    return new ScheduleRequest(
        doctorId,
        request.department_id(),
        request.schedule_date(),
        request.period(),
        request.start_time(),
        request.end_time(),
        request.total_count(),
        request.fee(),
        request.remark());
  }

  private DoctorSchedule toEntity(ScheduleRequest request) {
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

  private ScheduleResult toScheduleResult(DoctorSchedule schedule) {
    return new ScheduleResult(
        schedule.getId(),
        schedule.getDoctorId(),
        schedule.getDepartmentId(),
        schedule.getScheduleDate(),
        schedule.getPeriod(),
        schedule.getStartTime(),
        schedule.getEndTime(),
        schedule.getTotalCount(),
        schedule.getBookedCount(),
        schedule.getFee(),
        schedule.getStatus(),
        schedule.getRemark());
  }

  private SlotResult toSlotResult(ScheduleSlot slot) {
    return new SlotResult(
        slot.getId(),
        slot.getScheduleId(),
        slot.getSlotNo(),
        slot.getStartTime(),
        slot.getEndTime(),
        slot.getStatus());
  }

  private AuthenticatedUser currentDoctor(HttpServletRequest request) {
    AuthenticatedUser user = SessionAuth.require(request);
    if (user.doctor_id() == null
        || user.role_codes().stream().noneMatch("DOCTOR"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "当前账号不是医生");
    return user;
  }

  private AuthenticatedUser requireAdmin(HttpServletRequest request) {
    AuthenticatedUser user = SessionAuth.require(request);
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, "只有管理员可以执行该操作");
    return user;
  }
}
