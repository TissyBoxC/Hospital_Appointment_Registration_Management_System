package io.github.tissyboxc.harmsys.users;

import io.github.tissyboxc.harmsys.users.dto.PatientRegisterRequest;
import io.github.tissyboxc.harmsys.users.dto.RegisterResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
/** 患者自助注册接口。 */
public class UserAccountController {

  private final UserAccountService userAccountService;

  public UserAccountController(UserAccountService userAccountService) {
    this.userAccountService = userAccountService;
  }

  /**
   * 注册接口
   * @param request 下游请求体,包含注册信息
   */
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public RegisterResult register(@Valid @RequestBody PatientRegisterRequest request) {
    return userAccountService.PatientReg(request);
  }
}
