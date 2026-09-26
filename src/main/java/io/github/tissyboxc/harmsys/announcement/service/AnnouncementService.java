package io.github.tissyboxc.harmsys.announcement.service;

import io.github.tissyboxc.harmsys.announcement.repository.AnnouncementRepository;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 公告业务逻辑。 */
@Service
public class AnnouncementService {
  private final AnnouncementRepository repository;

  public AnnouncementService(AnnouncementRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> publicList() {
    return repository.findPublished();
  }

  @Transactional(readOnly = true)
  public Map<String, Object> publicGet(long id) {
    Map<String, Object> result = repository.findPublishedById(id);
    if (result == null) throw new UserRegistrationException(404, "公告不存在");
    return result;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> adminList(AuthenticatedUser user) {
    requireAdmin(user, "只有管理员可以管理公告");
    return repository.findAll();
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> create(AuthenticatedUser user, Map<String, Object> body) {
    requireAdmin(user, "只有管理员可以管理公告");
    long id = repository.insert(required(body, "title"), required(body, "content"), user.user_id());
    return repository.findById(id);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> update(
      AuthenticatedUser user, long id, Map<String, Object> body) {
    requireAdmin(user, "只有管理员可以管理公告");
    if (repository.updateDraft(id, required(body, "title"), required(body, "content")) != 1)
      throw new UserRegistrationException(409, "公告不存在或已发布");
    return repository.findById(id);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> publish(AuthenticatedUser user, long id) {
    requireAdmin(user, "只有管理员可以管理公告");
    if (repository.publish(id) != 1)
      throw new UserRegistrationException(409, "公告当前不能发布");
    return repository.findById(id);
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> retract(AuthenticatedUser user, long id) {
    requireAdmin(user, "只有管理员可以管理公告");
    if (repository.retract(id) != 1)
      throw new UserRegistrationException(409, "公告当前不能撤回");
    return repository.findById(id);
  }

  private void requireAdmin(AuthenticatedUser user, String message) {
    if (user.role_codes().stream().noneMatch("ADMIN"::equalsIgnoreCase))
      throw new SessionAuthenticationException(403, message);
  }

  private String required(Map<String, Object> body, String key) {
    Object value = body.get(key);
    if (value == null || String.valueOf(value).isBlank())
      throw new UserRegistrationException(422, key + "不能为空");
    return String.valueOf(value).trim();
  }
}

