package io.github.tissyboxc.harmsys.users;

/** 表示注册或业务校验失败并携带 HTTP 状态码。 */
public class UserRegistrationException extends RuntimeException {

  private final int code;

  public UserRegistrationException(int code, String message) {
    super(message);
    this.code = code;
  }

  public int getCode() {
    return this.code;
  }
}
