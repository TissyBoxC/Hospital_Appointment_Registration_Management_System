package io.github.tissyboxc.harmsys.users.service.impl;

import io.github.tissyboxc.harmsys.users.service.UserAccountService;

import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.operationlog.mapper.OperationLogMapper;
import io.github.tissyboxc.harmsys.users.mapper.UserAccountMapper;

import io.github.tissyboxc.harmsys.users.dto.PatientRegisterRequest;
import io.github.tissyboxc.harmsys.users.dto.RegisterResult;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 处理患者自助注册及初始角色分配。 */
public class UserAccountServiceImpl implements UserAccountService {
  private final UserAccountMapper mapper;
  private final OperationLogMapper operationLogMapper;
  private final PasswordEncoder passwordEncoder;

  public UserAccountServiceImpl(
      UserAccountMapper mapper,
      OperationLogMapper operationLogMapper,
      PasswordEncoder passwordEncoder) {
    this.mapper = mapper;
    this.operationLogMapper = operationLogMapper;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * 患者自助注册
   * @param request 注册信息
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public RegisterResult PatientReg(PatientRegisterRequest request) {
    String username = request.username().trim().toLowerCase(Locale.ROOT);

    if (!request.password().equals(request.confirm_password())) {
      throw new UserRegistrationException(400, "两次输入密码不一致");
    }

    if (mapper.countUsername(username) > 0) {
      throw new UserRegistrationException(409, "用户名已存在");
    }

    if (mapper.countIdCard(request.id_card()) > 0) {
      throw new UserRegistrationException(409, "该身份证号已注册过用户");
    }

    Long patientRoleId = mapper.selectPatientRoleId();
    if (patientRoleId == null)
      throw new IllegalStateException("系统没有初始化PATIENT角色");

    String passwordHash = passwordEncoder.encode(request.password());

    try {
      mapper.insertUser(username, passwordHash);
      Long generatedUserId = mapper.selectLastInsertId();
      if (generatedUserId == null) throw new IllegalStateException("创建用户账号失败");
      long userId = generatedUserId;

      mapper.insertPatient(
          userId,
          request.real_name(),
          request.id_card(),
          request.gender() == null ? 0 : request.gender(),
          request.birthday(),
          request.phone(),
          request.address(),
          request.emergency_contact(),
          request.emergency_phone());
      Long generatedPatientId = mapper.selectLastInsertId();
      if (generatedPatientId == null) throw new IllegalStateException("创建患者资料失败");
      long patientId = generatedPatientId;

      mapper.insertUserRole(userId, patientRoleId);

      OperationLog log = new OperationLog();
      log.setUserId(userId);
      log.setOperationType("REGISTER_PATIENT");
      log.setTargetType("patient");
      log.setTargetId(patientId);
      log.setDescription("患者完成账号注册");
      operationLogMapper.insert(log);

      return new RegisterResult(userId, patientId, username);
    } catch (DuplicateKeyException e) {
      throw new UserRegistrationException(409, "用户名或身份证已经注册过");
    }
  }
}

