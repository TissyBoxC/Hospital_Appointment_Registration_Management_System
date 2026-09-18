package io.github.tissyboxc.harmsys.users;

/** 表示注册或业务校验失败并携带 HTTP 状态码。 */
public class UserRegistrationException extends RuntimeException {

  private final int code;

  /**
   * 用户注册报错信息
   * @param code 错误码
   * @param message 错误信息
   */
  public UserRegistrationException(int code, String message) {
    super(message);
    this.code = code;
  }

  public int getCode() {
    return this.code;
  }
}
