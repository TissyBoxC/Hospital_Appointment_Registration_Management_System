package io.github.tissyboxc.harmsys.users.service;

import io.github.tissyboxc.harmsys.common.UserLoginException;

import io.github.tissyboxc.harmsys.users.dto.LoginRequest;
import io.github.tissyboxc.harmsys.users.dto.LoginResult;
import io.github.tissyboxc.harmsys.users.dto.LoginUserRecord;
import io.github.tissyboxc.harmsys.security.session.ActiveSessionService;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

/** 校验账号密码、建立会话并加载用户权限。 */
public interface UserLoginService {
public LoginResult login(LoginRequest request, HttpServletRequest httpRequest);

  public LoginResult currentUser(HttpServletRequest request);

  public void logout(HttpServletRequest request);
}
