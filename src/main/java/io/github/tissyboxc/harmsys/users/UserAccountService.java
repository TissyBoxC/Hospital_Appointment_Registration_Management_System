package io.github.tissyboxc.harmsys.users;

import io.github.tissyboxc.harmsys.users.dto.PatientRegisterRequest;
import io.github.tissyboxc.harmsys.users.dto.RegisterResult;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 处理患者自助注册及初始角色分配。 */
public class UserAccountService {
  private final UserAccountRepository repository;
  private final PasswordEncoder passwordEncoder;

  public UserAccountService(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * 患者自助注册
   * @param request 注册信息
   */
  @Transactional(rollbackFor = Exception.class)
  public RegisterResult PatientReg(PatientRegisterRequest request) {
    String username = request.username().trim().toLowerCase(Locale.ROOT);

    if (!request.password().equals(request.confirm_password())) {
      throw new UserRegistrationException(400, "两次输入密码不一致");
    }

    if (repository.usernameExists(username)) {
      throw new UserRegistrationException(409, "用户名已存在");
    }

    if (repository.idCardExists(request.id_card())) {
      throw new UserRegistrationException(409, "该身份证号已注册过用户");
    }

    Long patientRoleId =
        repository
            .findPatientRoleId()
            .orElseThrow(() -> new IllegalStateException("系统没有初始化PATIENT角色"));

    String passwordHash = passwordEncoder.encode(request.password());

    try {
      long userId = repository.insertUser(username, passwordHash);

      long patientId = repository.insertPatient(userId, request);

      repository.assignRole(userId, patientRoleId);

      repository.writeRegisterLog(userId, patientId);

      return new RegisterResult(userId, patientId, username);
    } catch (DuplicateKeyException e) {
      throw new UserRegistrationException(409, "用户名或身份证已经注册过");
    }
  }
}
