package io.github.tissyboxc.harmsys.admin.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 管理员用户、角色、权限和统计数据访问。 */
public interface AdminAccessMapper {

  int updateUser(
      @Param("id") long id,
      @Param("username") String username,
      @Param("status") Integer status);

  Map<String, Object> selectUser(@Param("id") long id);

  int deleteUser(@Param("id") long id);

  int deletePatientByUser(@Param("userId") long userId);

  int deleteDoctorByUser(@Param("userId") long userId);

  int restoreUser(@Param("id") long id);

  int restorePatientByUser(@Param("userId") long userId);

  int restoreDoctorByUser(@Param("userId") long userId);

  Map<String, Object> selectRestoredUser(@Param("id") long id);

  Long selectRoleId(@Param("roleCode") String roleCode);

  int insertUserRole(@Param("userId") long userId, @Param("roleId") long roleId);

  Map<String, Object> selectUserRole(
      @Param("userId") long userId, @Param("roleId") long roleId);

  int deleteUserRole(@Param("userId") long userId, @Param("roleId") long roleId);

  Long selectPermissionId(@Param("permissionCode") String permissionCode);

  int insertRolePermission(
      @Param("roleId") long roleId, @Param("permissionId") long permissionId);

  Map<String, Object> selectRolePermission(
      @Param("roleId") long roleId, @Param("permissionId") long permissionId);

  int deleteRolePermission(
      @Param("roleId") long roleId, @Param("permissionId") long permissionId);

  long countActiveUser(@Param("userId") long userId);

  int insertUserPermission(
      @Param("userId") long userId,
      @Param("permissionId") long permissionId,
      @Param("grantedBy") long grantedBy);

  Map<String, Object> selectUserDirectPermission(
      @Param("userId") long userId, @Param("permissionId") long permissionId);

  int deleteUserPermission(
      @Param("userId") long userId, @Param("permissionId") long permissionId);

  List<Map<String, Object>> selectUserDirectPermissions(@Param("userId") long userId);

  int insertRole(
      @Param("code") String code, @Param("name") String name, @Param("status") int status);

  Long selectRoleIdByCode(@Param("code") String code);

  List<Map<String, Object>> selectRoles();

  Map<String, Object> selectRole(@Param("id") long id);

  List<Map<String, Object>> selectRolePermissions(@Param("roleId") long roleId);

  int updateRole(
      @Param("id") long id, @Param("name") String name, @Param("status") int status);

  int disableRole(@Param("id") long id);

  int insertPermission(
      @Param("code") String code, @Param("name") String name, @Param("type") int type);

  Long selectPermissionIdByCode(@Param("code") String code);

  List<Map<String, Object>> selectPermissions();

  Map<String, Object> selectPermission(@Param("id") long id);

  int updatePermission(
      @Param("id") long id, @Param("name") String name, @Param("type") int type);

  long countPermission(@Param("id") long id);

  int deletePermissionRelations(@Param("id") long id);

  int deletePermission(@Param("id") long id);

  long countUsers();

  long countPatients();

  long countDoctors();

  long countDepartments();

  long countTodayAppointments();

  long countTodayCompletedVisits();

  BigDecimal selectPaidAmount();
}
