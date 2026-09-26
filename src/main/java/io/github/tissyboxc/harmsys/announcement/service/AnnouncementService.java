package io.github.tissyboxc.harmsys.announcement.service;

import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import java.util.List;
import java.util.Map;

/** 公告业务。 */
public interface AnnouncementService {

  List<Map<String, Object>> publicList();

  Map<String, Object> publicGet(long id);

  List<Map<String, Object>> adminList(AuthenticatedUser user);

  Map<String, Object> create(AuthenticatedUser user, Map<String, Object> body);

  Map<String, Object> update(AuthenticatedUser user, long id, Map<String, Object> body);

  Map<String, Object> publish(AuthenticatedUser user, long id);

  Map<String, Object> retract(AuthenticatedUser user, long id);
}
