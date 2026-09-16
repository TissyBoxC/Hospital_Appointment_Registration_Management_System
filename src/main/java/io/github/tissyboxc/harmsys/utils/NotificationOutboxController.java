package io.github.tissyboxc.harmsys.utils;

import io.github.tissyboxc.harmsys.users.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.users.UserRegistrationException;
import io.github.tissyboxc.harmsys.users.sessions.AuthenticatedUser;
import io.github.tissyboxc.harmsys.users.sessions.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** 外部短信/邮件队列管理。发送为模拟动作，后续可替换为真实供应商。 */
@RestController
@RequestMapping("/api/admin/notification-outbox")
/** 管理员查看和重试待发送通知的接口。 */
public class NotificationOutboxController {
  private final JdbcTemplate jdbc;

  public NotificationOutboxController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping
  public List<Map<String, Object>> list(
      @RequestParam(required = false) Integer status,
      @RequestParam(defaultValue = "100") int limit,
      HttpServletRequest request) {
    admin(request);
    int size = Math.min(Math.max(1, limit), 500);
    if (status == null)
      return jdbc.queryForList("SELECT * FROM notification_outbox ORDER BY id DESC LIMIT ?", size);
    if (status < 0 || status > 2) throw new UserRegistrationException(422, "队列状态只能为0到2");
    return jdbc.queryForList(
        "SELECT * FROM notification_outbox WHERE status=? ORDER BY id DESC LIMIT ?", status, size);
  }

  @PostMapping("/{id}/send")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional(rollbackFor = Exception.class)
  public void send(@PathVariable long id, HttpServletRequest request) {
    AuthenticatedUser u = admin(request);
    if (jdbc.update(
            "UPDATE notification_outbox SET status=1,sent_at=CURRENT_TIMESTAMP,error_message=NULL"
                + " WHERE id=? AND status IN (0,2)",
            id)
        != 1) throw new UserRegistrationException(409, "队列消息不存在或当前不可发送");
    log(u.user_id(), "SEND_NOTIFICATION_OUTBOX", id, "模拟发送外部通知", request);
  }

  @PostMapping("/{id}/retry")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional(rollbackFor = Exception.class)
  public void retry(@PathVariable long id, HttpServletRequest request) {
    AuthenticatedUser u = admin(request);
    if (jdbc.update(
            "UPDATE notification_outbox SET status=0,error_message=NULL,sent_at=NULL WHERE id=? AND"
                + " status=2",
            id)
        != 1) throw new UserRegistrationException(409, "只有失败消息可以重试");
    log(u.user_id(), "RETRY_NOTIFICATION_OUTBOX", id, "重新加入外部通知发送队列", request);
  }

  private AuthenticatedUser admin(HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    if (u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("ADMIN")))
      throw new SessionAuthenticationException(403, "只有管理员可以管理外部通知队列");
    return u;
  }

  private void log(long uid, String type, long id, String desc, HttpServletRequest r) {
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,?,?,?,?)",
        uid,
        type,
        "notification_outbox",
        id,
        desc,
        r.getRemoteAddr());
  }
}
