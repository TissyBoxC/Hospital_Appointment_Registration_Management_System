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

  @ExceptionHandler(UserRegistrationException.class)
  public ResponseEntity<ErrorResponse> handle(UserRegistrationException exception) {
    return ResponseEntity.status(exception.getCode())
        .body(new ErrorResponse(exception.getCode(), exception.getMessage()));
  }

  @ExceptionHandler(UserLoginException.class)
  public ResponseEntity<ErrorResponse> handleLoginException(UserLoginException exception) {
    return ResponseEntity.status(exception.getCode())
        .body(new ErrorResponse(exception.getCode(), exception.getMessage()));
  }

  @ExceptionHandler(SessionAuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleSessionException(
      SessionAuthenticationException exception) {
    return ResponseEntity.status(exception.getCode())
        .body(new ErrorResponse(exception.getCode(), exception.getMessage()));
  }

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

  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ErrorResponse> handleDatabase(DataAccessException exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse(500, "数据库操作失败"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse(500, "服务器内部错误"));
  }
}
