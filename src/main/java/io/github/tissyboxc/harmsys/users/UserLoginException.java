package io.github.tissyboxc.harmsys.users;

/** 表示登录账号、密码或状态校验失败。 */
public class UserLoginException extends RuntimeException {

  private final int code;

  /**
   * 登陆异常
   * @param code 错误码
   * @param message 错误信息
   */
  public UserLoginException(int code, String message) {
    super(message);
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
