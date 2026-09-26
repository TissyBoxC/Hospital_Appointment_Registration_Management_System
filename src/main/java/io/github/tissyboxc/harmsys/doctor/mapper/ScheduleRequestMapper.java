package io.github.tissyboxc.harmsys.doctor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.tissyboxc.harmsys.doctor.dto.ScheduleRequestResult;
import io.github.tissyboxc.harmsys.doctor.entity.DoctorScheduleRequest;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** doctor_schedule_request 表数据访问。 */
public interface ScheduleRequestMapper extends BaseMapper<DoctorScheduleRequest> {

  long countPendingConflicts(
      @Param("doctorId") long doctorId,
      @Param("targetScheduleId") Long targetScheduleId,
      @Param("scheduleDate") LocalDate scheduleDate,
      @Param("period") int period);

  List<ScheduleRequestResult> selectMine(
      @Param("doctorId") long doctorId, @Param("status") Integer status);

  List<ScheduleRequestResult> selectReviewList(
      @Param("admin") boolean admin,
      @Param("departmentManager") boolean departmentManager,
      @Param("hasReviewPermission") boolean hasReviewPermission,
      @Param("operatorDepartmentId") Long operatorDepartmentId,
      @Param("operatorUserId") long operatorUserId,
      @Param("status") Integer status,
      @Param("departmentId") Long departmentId);

  ScheduleRequestResult selectResult(@Param("id") long id);

  DoctorScheduleRequest lockRequest(@Param("id") long id);

  DoctorScheduleRequest lockDoctorRequest(
      @Param("id") long id, @Param("doctorId") long doctorId);

  long countDepartmentManager(
      @Param("userId") long userId, @Param("departmentId") Long departmentId);

  int cancelRequest(@Param("id") long id);

  int rejectRequest(
      @Param("id") long id,
      @Param("operatorUserId") long operatorUserId,
      @Param("reviewRemark") String reviewRemark);

  int approveRequest(
      @Param("id") long id,
      @Param("operatorUserId") long operatorUserId,
      @Param("reviewRemark") String reviewRemark,
      @Param("scheduleId") Long scheduleId);
}
