package io.github.tissyboxc.harmsys.users;

import io.github.tissyboxc.harmsys.users.dto.LoginUserRecord;
import io.github.tissyboxc.harmsys.users.dto.PatientRegisterRequest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
/** 患者注册、账号校验和登录信息加载的数据访问层。 */
public class UserAccountRepository {
  private final JdbcTemplate jdbcTemplate;

  public UserAccountRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public boolean usernameExists(String username) {
    Long count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM sys_user
            WHERE username=?
            """,
            Long.class,
            username);
    return count != null && count > 0;
  }

  public boolean idCardExists(String idCard) {
    Long count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM patient
            WHERE id_card=?
            """,
            Long.class,
            idCard);
    return count != null && count > 0;
  }

  public long insertUser(String username, String passwordHash) {
    String sql =
        """
        INSERT INTO sys_user(
            username,
            password_hash,
            user_type,
            status,
            deleted
        )VALUES(?,?,1,1,0)
        """;
    KeyHolder keyHolder = new GeneratedKeyHolder();

    int rows =
        jdbcTemplate.update(
            con -> {
              PreparedStatement statement =
                  con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
              statement.setString(1, username);
              statement.setString(2, passwordHash);

              return statement;
            },
            keyHolder);

    if (rows != 1 || keyHolder.getKey() == null) {
      throw new IllegalStateException("创建用户账号失败");
    }
    return keyHolder.getKey().longValue();
  }

  public long insertPatient(long userId, PatientRegisterRequest request) {
    String sql =
        """
        INSERT INTO patient (
            user_id,
            real_name,
            id_card,
            gender,
            birthday,
            phone,
            address,
            emergency_contact,
            emergency_phone,
            deleted
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
        """;

    KeyHolder keyHolder = new GeneratedKeyHolder();

    int rows =
        jdbcTemplate.update(
            connection -> {
              PreparedStatement statement =
                  connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

              statement.setLong(1, userId);
              statement.setString(2, request.real_name());
              statement.setString(3, request.id_card());

              statement.setInt(4, request.gender() == null ? 0 : request.gender());

              statement.setObject(5, request.birthday());
              statement.setString(6, request.phone());
              statement.setString(7, request.address());
              statement.setString(8, request.emergency_contact());
              statement.setString(9, request.emergency_phone());

              return statement;
            },
            keyHolder);

    if (rows != 1 || keyHolder.getKey() == null) {
      throw new IllegalStateException("创建患者资料失败");
    }

    return keyHolder.getKey().longValue();
  }

  public Optional<Long> findPatientRoleId() {
    return jdbcTemplate
        .query(
            """
            SELECT id
            FROM sys_role
            WHERE role_code = 'PATIENT'
              AND status = 1
            LIMIT 1
            """,
            (resultSet, rowNumber) -> resultSet.getLong("id"))
        .stream()
        .findFirst();
  }

  public void assignRole(long userId, long roleId) {
    int rows =
        jdbcTemplate.update(
            """
            INSERT INTO sys_user_role (
                user_id,
                role_id
            ) VALUES (?, ?)
            """,
            userId,
            roleId);

    if (rows != 1) {
      throw new IllegalStateException("分配患者角色失败");
    }
  }

  public void writeRegisterLog(long userId, long patientId) {
    jdbcTemplate.update(
        """
        INSERT INTO operation_log (
            user_id,
            operation_type,
            target_type,
            target_id,
            description
        ) VALUES (?, 'REGISTER_PATIENT', 'patient', ?, ?)
        """,
        userId,
        patientId,
        "患者完成账号注册");
  }

  public Optional<LoginUserRecord> findLoginUser(String username) {
    String sql =
        """
        SELECT
            u.id AS user_id,
            u.username,
            u.password_hash,
            u.user_type,
            u.status,
            p.id AS patient_id,
            d.id AS doctor_id,
            d.status AS doctor_status,
            COALESCE(
                p.real_name,
                d.real_name,
                u.username
            ) AS display_name
        FROM sys_user u
        LEFT JOIN patient p
               ON p.user_id = u.id
              AND p.deleted = 0
        LEFT JOIN doctor d
               ON d.user_id = u.id
              AND d.deleted = 0
        WHERE u.username = ?
          AND u.deleted = 0
        LIMIT 1
        """;

    return jdbcTemplate
        .query(
            sql,
            (resultSet, rowNumber) ->
                new LoginUserRecord(
                    resultSet.getLong("user_id"),
                    resultSet.getString("username"),
                    resultSet.getString("password_hash"),
                    resultSet.getObject("user_type", Integer.class),
                    resultSet.getObject("status", Integer.class),
                    getNullableLong(resultSet, "patient_id"),
                    getNullableLong(resultSet, "doctor_id"),
                    resultSet.getObject("doctor_status", Integer.class),
                    resultSet.getString("display_name")),
            username)
        .stream()
        .findFirst();
  }

  public List<String> findRoleCodes(long userId) {
    return jdbcTemplate.query(
        """
        SELECT r.role_code
        FROM sys_user_role ur
        INNER JOIN sys_role r
                ON r.id = ur.role_id
        WHERE ur.user_id = ?
          AND r.status = 1
        ORDER BY r.id
        """,
        (resultSet, rowNumber) -> resultSet.getString("role_code"),
        userId);
  }

  public void updateLastLoginTime(long userId) {
    jdbcTemplate.update(
        """
        UPDATE sys_user
        SET last_login_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """,
        userId);
  }

  private Long getNullableLong(java.sql.ResultSet resultSet, String columnName)
      throws java.sql.SQLException {

    long value = resultSet.getLong(columnName);

    return resultSet.wasNull() ? null : value;
  }
}
