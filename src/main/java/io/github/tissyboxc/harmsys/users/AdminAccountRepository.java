package io.github.tissyboxc.harmsys.users;

import io.github.tissyboxc.harmsys.users.dto.AdminCreateDoctorRequest;
import io.github.tissyboxc.harmsys.users.dto.AdminCreatePatientRequest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
/** 管理员创建不同类型账号时的数据访问层。 */
public class AdminAccountRepository {
  private final JdbcTemplate jdbcTemplate;

  public AdminAccountRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

    /**
     * 用户名查重
     */
  public boolean usernameExists(String username) {
    return count("SELECT COUNT(*) FROM sys_user WHERE username = ?", username) > 0;
  }

    /**
     * 身份证号查重
     */
  public boolean idCardExists(String idCard) {
    return count("SELECT COUNT(*) FROM patient WHERE id_card = ?", idCard) > 0;
  }

    /**
     * 医生查重
     */
  public boolean doctorNoExists(String doctorNo) {
    return count("SELECT COUNT(*) FROM doctor WHERE doctor_no = ?", doctorNo) > 0;
  }

    /**
     * 科室启用状态
     */
  public boolean departmentEnabled(long departmentId) {
    return count(
            "SELECT COUNT(*) FROM department WHERE id = ? AND status = 1 AND deleted = 0",
            departmentId)
        > 0;
  }

    /**
     * 新建用户
     * @param username 用户名
     * @param passwordHash 密码
     * @param userType 角色类型
     */
  public long insertUser(String username, String passwordHash, int userType) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    int rows =
        jdbcTemplate.update(
            connection -> {
              PreparedStatement statement =
                  connection.prepareStatement(
                      "INSERT INTO sys_user(username,password_hash,user_type,status,deleted)"
                          + " VALUES(?,?,?,1,0)",
                      Statement.RETURN_GENERATED_KEYS);
              statement.setString(1, username);
              statement.setString(2, passwordHash);
              statement.setInt(3, userType);
              return statement;
            },
            keyHolder);
    if (rows != 1 || keyHolder.getKey() == null) {
      throw new IllegalStateException("创建用户账号失败");
    }
    return keyHolder.getKey().longValue();
  }

    /**
     * 新建病人
     * @param userId 用户ID
     * @param request 创建请求体,包含信息
     */
  public long insertPatient(long userId, AdminCreatePatientRequest request) {
    return insertProfile(
        "INSERT INTO"
            + " patient(user_id,real_name,id_card,gender,birthday,phone,address,emergency_contact,emergency_phone,deleted)"
            + " VALUES(?,?,?,?,?,?,?,?,?,0)",
        statement -> {
          statement.setLong(1, userId);
          statement.setString(2, request.real_name());
          statement.setString(3, request.id_card());
          statement.setInt(4, request.gender() == null ? 0 : request.gender());
          statement.setObject(5, request.birthday());
          statement.setString(6, request.phone());
          statement.setString(7, request.address());
          statement.setString(8, request.emergency_contact());
          statement.setString(9, request.emergency_phone());
        });
  }

    /**
     * 新建医生
     * @param userId 用户ID
     * @param request 创建请求体,包含信息
     * @return
     */
  public long insertDoctor(long userId, AdminCreateDoctorRequest request) {
    return insertProfile(
        "INSERT INTO"
            + " doctor(user_id,department_id,doctor_no,real_name,title,specialty,introduction,avatar_url,consultation_fee,status,deleted)"
            + " VALUES(?,?,?,?,?,?,?,?,?,1,0)",
        statement -> {
          statement.setLong(1, userId);
          statement.setLong(2, request.department_id());
          statement.setString(3, request.doctor_no());
          statement.setString(4, request.real_name());
          statement.setString(5, request.title());
          statement.setString(6, request.specialty());
          statement.setString(7, request.introduction());
          statement.setString(8, request.avatar_url());
          statement.setBigDecimal(9, request.consultation_fee());
        });
  }

    /**
     * 查询角色ID
     * @param roleCode 角色代码
     */
  public Optional<Long> findRoleId(String roleCode) {
    return jdbcTemplate
        .query(
            "SELECT id FROM sys_role WHERE role_code = ? AND status = 1 LIMIT 1",
            (rs, row) -> rs.getLong("id"),
            roleCode)
        .stream()
        .findFirst();
  }

    /**
     * 新建角色与用户关系
     * @param userId 用户ID
     * @param roleId 角色ID
     */
  public void assignRole(long userId, long roleId) {
    jdbcTemplate.update("INSERT INTO sys_user_role(user_id,role_id) VALUES(?,?)", userId, roleId);
  }

    /**
     * 删除关系
     */
  public void removeRole(long userId, long roleId) {
    jdbcTemplate.update(
        "DELETE FROM sys_user_role WHERE user_id = ? AND role_id = ?", userId, roleId);
  }

    /**
     * 用户已存在
     */
  public boolean userExists(long userId) {
    return count("SELECT COUNT(*) FROM sys_user WHERE id = ? AND deleted = 0", userId) > 0;
  }

    /**
     * 更新用户状态
     */
  public void updateStatus(long userId, int status) {
    if (jdbcTemplate.update(
            "UPDATE sys_user SET status = ? WHERE id = ? AND deleted = 0", status, userId)
        != 1) {
      throw new UserRegistrationException(404, "用户不存在或已删除");
    }
  }

    /**
     * 重置密码
     */
  public void resetPassword(long userId, String passwordHash) {
    if (jdbcTemplate.update(
            "UPDATE sys_user SET password_hash = ? WHERE id = ? AND deleted = 0",
            passwordHash,
            userId)
        != 1) {
      throw new UserRegistrationException(404, "用户不存在或已删除");
    }
  }

    /**
     * 统一日志逻辑
     */
  public void writeLog(
      long operatorId,
      String type,
      String targetType,
      Long targetId,
      String description,
      String ip) {
    jdbcTemplate.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,?,?,?,?)",
        operatorId,
        type,
        targetType,
        targetId,
        description,
        ip);
  }

  private long count(String sql, Object... args) {
    Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
    return value == null ? 0 : value;
  }

    /**
     * 新建资料
     */
  private long insertProfile(String sql, StatementBinder binder) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    int rows =
        jdbcTemplate.update(
            connection -> {
              PreparedStatement statement =
                  connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
              binder.bind(statement);
              return statement;
            },
            keyHolder);
    if (rows != 1 || keyHolder.getKey() == null) {
      throw new IllegalStateException("创建业务资料失败");
    }
    return keyHolder.getKey().longValue();
  }

  @FunctionalInterface
  private interface StatementBinder {
    void bind(PreparedStatement statement) throws java.sql.SQLException;
  }
}
