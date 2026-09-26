package io.github.tissyboxc.harmsys.security.mapper;

import org.apache.ibatis.annotations.Param;

/** 用户角色与直授权限查询。 */
public interface PermissionMapper {

  long countPermission(
      @Param("userId") long userId, @Param("permissionCode") String permissionCode);
}
