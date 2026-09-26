package io.github.tissyboxc.harmsys.doctor.service;

import io.github.tissyboxc.harmsys.doctor.dto.DoctorProfileResult;
import io.github.tissyboxc.harmsys.doctor.dto.DoctorProfileUpdateRequest;
import io.github.tissyboxc.harmsys.doctor.dto.ScheduleRequest;
import io.github.tissyboxc.harmsys.doctor.dto.ScheduleResult;
import io.github.tissyboxc.harmsys.doctor.dto.SlotRequest;
import io.github.tissyboxc.harmsys.doctor.dto.SlotResult;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

/** 医生资料、排班和时间段维护业务。 */
public interface DoctorService {

  DoctorProfileResult profile(HttpServletRequest request);

  DoctorProfileResult updateProfile(DoctorProfileUpdateRequest update, HttpServletRequest request);

  List<ScheduleResult> schedules(HttpServletRequest request);

  List<ScheduleResult> allSchedules(HttpServletRequest request);

  ScheduleResult schedule(long scheduleId);

  ScheduleResult adminSchedule(long scheduleId, HttpServletRequest request);

  ScheduleResult scheduleForDoctor(long scheduleId, HttpServletRequest request);

  ScheduleResult createOwnSchedule(ScheduleRequest request, HttpServletRequest httpRequest);

  ScheduleResult createAdminSchedule(ScheduleRequest request, HttpServletRequest httpRequest);

  ScheduleResult updateOwnSchedule(
      long scheduleId, ScheduleRequest request, HttpServletRequest httpRequest);

  ScheduleResult updateAdminSchedule(
      long scheduleId, ScheduleRequest request, HttpServletRequest httpRequest);

  void deleteOwnSchedule(long scheduleId, HttpServletRequest request);

  void deleteAdminSchedule(long scheduleId, HttpServletRequest request);

  List<SlotResult> adminSlots(long scheduleId, HttpServletRequest request);

  SlotResult createAdminSlot(long scheduleId, SlotRequest slot, HttpServletRequest request);

  void adminUpdateSlotStatus(long slotId, int status, HttpServletRequest request);

  List<SlotResult> slots(long scheduleId, HttpServletRequest request);

  SlotResult createOwnSlot(long scheduleId, SlotRequest slot, HttpServletRequest request);

  void updateSlotStatus(long slotId, int status, HttpServletRequest request);

  Optional<DoctorProfileResult> findProfile(long doctorId);

  Optional<Long> findDoctorDepartment(long doctorId);

  boolean doctorEnabled(long doctorId);

  boolean departmentMatchesDoctor(long doctorId, long departmentId);

  boolean scheduleBelongsTo(long scheduleId, long doctorId);

  Optional<ScheduleResult> findSchedule(long id);

  List<SlotResult> findSlots(long scheduleId);

  Optional<SlotResult> findSlot(long slotId);

  long insertSlot(long scheduleId, SlotRequest request);

  void updateSlotStatus(long slotId, int status);

  boolean slotOverlaps(
      long scheduleId, java.time.LocalTime startTime, java.time.LocalTime endTime);

  boolean slotNumberExists(long scheduleId, int slotNo);

  void writeLog(
      long userId,
      String operationType,
      String targetType,
      Long targetId,
      String description,
      String ipAddress);
}
