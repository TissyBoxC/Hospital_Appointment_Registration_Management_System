package io.github.tissyboxc.harmsys.announcement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.tissyboxc.harmsys.announcement.entity.Announcement;
import io.github.tissyboxc.harmsys.announcement.mapper.AnnouncementMapper;
import io.github.tissyboxc.harmsys.announcement.service.AnnouncementService;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 公告业务实现。 */
@Service
public class AnnouncementServiceImpl implements AnnouncementService {
  private final AnnouncementMapper mapper;

  public AnnouncementServiceImpl(AnnouncementMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Map<String, Object>> publicList() {
    return mapper
        .selectList(
            new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, 1)
                .orderByDesc(Announcement::getPublishedAt)
                .orderByDesc(Announcement::getId))
        .stream()
        .map(this::publishedResult)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Object> publicGet(long id) {
    Announcement announcement =
        mapper.selectOne(
            new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getId, id)
                .eq(Announcement::getStatus, 1));
    if (announcement == null) throw new UserRegistrationException(404, "公告不存在");
    return publishedResult(announcement);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Map<String, Object>> adminList(AuthenticatedUser user) {
    requireAdmin(user, "只有管理员可以管理公告");
    return mapper
        .selectList(new LambdaQueryWrapper<Announcement>().orderByDesc(Announcement::getId))
        .stream()
        .map(this::toResult)
        .toList();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> create(AuthenticatedUser user, Map<String, Object> body) {
    requireAdmin(user, "只有管理员可以管理公告");
    Announcement announcement = new Announcement();
    announcement.setTitle(required(body, "title"));
    announcement.setContent(required(body, "content"));
    announcement.setStatus(0);
    announcement.setPublisherUserId(user.user_id());
    mapper.insert(announcement);
    return toResult(requireAnnouncement(announcement.getId()));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> update(
      AuthenticatedUser user, long id, Map<String, Object> body) {
    requireAdmin(user, "只有管理员可以管理公告");
    Announcement announcement = requireAnnouncement(id);
    if (announcement.getStatus() != 0)
      throw new UserRegistrationException(409, "公告不存在或已发布");
    announcement.setTitle(required(body, "title"));
    announcement.setContent(required(body, "content"));
    mapper.updateById(announcement);
    return toResult(requireAnnouncement(id));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> publish(AuthenticatedUser user, long id) {
    requireAdmin(user, "只有管理员可以管理公告");
    Announcement announcement = requireAnnouncement(id);
    if (announcement.getStatus() != 0)
      throw new UserRegistrationException(409, "公告当前不能发布");
    announcement.setStatus(1);
    announcement.setPublishedAt(LocalDateTime.now());
    mapper.updateById(announcement);
    return toResult(requireAnnouncement(id));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> retract(AuthenticatedUser user, long id) {
    requireAdmin(user, "只有管理员可以管理公告");
    Announcement announcement = requireAnnouncement(id);
    if (announcement.getStatus() != 1)
      throw new UserRegistrationException(409, "公告当前不能撤回");
    announcement.setStatus(2);
    mapper.updateById(announcement);
    return toResult(requireAnnouncement(id));
  }

  private Announcement requireAnnouncement(long id) {
    Announcement announcement = mapper.selectById(id);
    if (announcement == null) throw new UserRegistrationException(404, "公告不存在");
    return announcement;
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

  private Map<String, Object> toResult(Announcement announcement) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", announcement.getId());
    result.put("title", announcement.getTitle());
    result.put("content", announcement.getContent());
    result.put("status", announcement.getStatus());
    result.put("publisher_user_id", announcement.getPublisherUserId());
    result.put("published_at", announcement.getPublishedAt());
    result.put("created_at", announcement.getCreatedAt());
    result.put("updated_at", announcement.getUpdatedAt());
    return result;
  }

  private Map<String, Object> publishedResult(Announcement announcement) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", announcement.getId());
    result.put("title", announcement.getTitle());
    result.put("content", announcement.getContent());
    result.put("published_at", announcement.getPublishedAt());
    return result;
  }
}
