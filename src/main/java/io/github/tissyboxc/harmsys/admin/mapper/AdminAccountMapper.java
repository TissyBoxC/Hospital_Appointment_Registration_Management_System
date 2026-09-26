package io.github.tissyboxc.harmsys.admin.mapper;

import io.github.tissyboxc.harmsys.admin.dto.AdminCreateDoctorRequest;
import io.github.tissyboxc.harmsys.admin.dto.AdminCreatePatientRequest;
import org.apache.ibatis.annotations.Param;

/** 管理员创建账号及关联资料的数据访问。 */
public interface AdminAccountMapper {

  long countUsername(@Param("username") String username);

  long countIdCard(@Param("idCard") String idCard);

  long countDoctorNo(@Param("doctorNo") String doctorNo);

  long countEnabledDepartment(@Param("departmentId") long departmentId);

  int insertUser(
      @Param("username") String username,
      @Param("passwordHash") String passwordHash,
      @Param("userType") int userType);

  Long selectLastInsertId();

  int insertPatient(
      @Param("userId") long userId, @Param("request") AdminCreatePatientRequest request);

  int insertDoctor(
      @Param("userId") long userId, @Param("request") AdminCreateDoctorRequest request);

  Long selectRoleId(@Param("roleCode") String roleCode);

  int insertUserRole(@Param("userId") long userId, @Param("roleId") long roleId);

  int deleteUserRole(@Param("userId") long userId, @Param("roleId") long roleId);

  long countActiveUser(@Param("userId") long userId);

  Long selectDoctorDepartmentByUserId(@Param("userId") long userId);

  int insertDepartmentManager(
      @Param("departmentId") long departmentId, @Param("userId") long userId);

  int deleteDepartmentManagers(@Param("userId") long userId);

  int updateStatus(@Param("userId") long userId, @Param("status") int status);

  int updatePassword(
      @Param("userId") long userId, @Param("passwordHash") String passwordHash);
}
