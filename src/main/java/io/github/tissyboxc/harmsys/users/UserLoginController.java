package io.github.tissyboxc.harmsys.users;

import io.github.tissyboxc.harmsys.users.dto.LoginRequest;
import io.github.tissyboxc.harmsys.users.dto.LoginResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
/** 用户登录、当前用户和退出登录接口。 */
public class UserLoginController {

  private final UserLoginService userLoginService;

  public UserLoginController(UserLoginService userLoginService) {
    this.userLoginService = userLoginService;
  }

  /**
   * 用户登录接口
   * @param request 下游请求体
   */
  @PostMapping("/login")
  @ResponseStatus(HttpStatus.OK)
  public LoginResult login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    return userLoginService.login(request, httpRequest);
  }

  /**
   * 用户进入主页,获取信息
   */
  @GetMapping("/me")
  public LoginResult currentUser(HttpServletRequest request) {
    return userLoginService.currentUser(request);
  }

  /**
   * 用户登出
   */
  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(HttpServletRequest request) {
    userLoginService.logout(request);
  }
}
