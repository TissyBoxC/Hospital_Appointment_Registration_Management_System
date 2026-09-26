package io.github.tissyboxc.harmsys.users.mapper;

import io.github.tissyboxc.harmsys.users.dto.LoginUserRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 用户账号、患者资料和登录信息的数据访问。 */
public interface UserAccountMapper {

  long countUsername(@Param("username") String username);

  long countIdCard(@Param("idCard") String idCard);

  int insertUser(
      @Param("username") String username, @Param("passwordHash") String passwordHash);

  Long selectLastInsertId();

  int insertPatient(
      @Param("userId") long userId,
      @Param("realName") String realName,
      @Param("idCard") String idCard,
      @Param("gender") Integer gender,
      @Param("birthday") Object birthday,
      @Param("phone") String phone,
      @Param("address") String address,
      @Param("emergencyContact") String emergencyContact,
      @Param("emergencyPhone") String emergencyPhone);

  Long selectPatientRoleId();

  int insertUserRole(
      @Param("userId") long userId, @Param("roleId") long roleId);

  LoginUserRecord selectLoginUser(@Param("username") String username);

  List<String> selectRoleCodes(@Param("userId") long userId);

  int updateLastLoginTime(@Param("userId") long userId);
}
