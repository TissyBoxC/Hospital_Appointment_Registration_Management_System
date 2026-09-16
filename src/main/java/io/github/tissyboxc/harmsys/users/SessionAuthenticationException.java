package io.github.tissyboxc.harmsys.users;

/** 表示登录状态或接口权限校验失败。 */
public class SessionAuthenticationException extends RuntimeException {

  private final int code;

  public SessionAuthenticationException(int code, String message) {
    super(message);
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
