package io.github.tissyboxc.harmsys.users;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
/** 将账号、认证和参数校验异常转换为统一响应。 */
public class UserRegistrationExceptionHandler {

  /** 统一错误响应结构。 */
  public record ErrorResponse(int code, String message) {}

  /**
   * 业务异常处理
   * @param exception 业务异常
   */
  @ExceptionHandler(UserRegistrationException.class)
  public ResponseEntity<ErrorResponse> handle(UserRegistrationException exception) {
    return ResponseEntity.status(exception.getCode())
        .body(new ErrorResponse(exception.getCode(), exception.getMessage()));
  }
  /**
   * 登录异常处理
   * @param exception 登录异常
   */
  @ExceptionHandler(UserLoginException.class)
  public ResponseEntity<ErrorResponse> handleLoginException(UserLoginException exception) {
    return ResponseEntity.status(exception.getCode())
        .body(new ErrorResponse(exception.getCode(), exception.getMessage()));
  }

  /**
   * 会话和权限处理
   * @param exception 会话和权限异常
   */
  @ExceptionHandler(SessionAuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleSessionException(
      SessionAuthenticationException exception) {
    return ResponseEntity.status(exception.getCode())
        .body(new ErrorResponse(exception.getCode(), exception.getMessage()));
  }

  /**
   * 请求参数不合法处理
   * @param exception 请求参数异常
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse("请求参数校验失败");
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(new ErrorResponse(422, message));
  }

  /**
   * 数据库异常处理
   * @param exception 数据库异常
   */
  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ErrorResponse> handleDatabase(DataAccessException exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse(500, "数据库操作失败"));
  }

  /**
   * 错误处理
   * @param exception 错误
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse(500, "服务器内部错误"));
  }
}
