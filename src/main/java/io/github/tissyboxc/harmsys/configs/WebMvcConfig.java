package io.github.tissyboxc.harmsys.configs;

import io.github.tissyboxc.harmsys.users.PermissionAuthorizationService;
import io.github.tissyboxc.harmsys.users.sessions.ActiveSessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
/** 注册拦截器、跨域规则和消息转换配置。 */
public class WebMvcConfig implements WebMvcConfigurer {

  private final PermissionAuthorizationService permissionAuthorizationService;
  private final ActiveSessionService activeSessionService;

  //放行地址
  @Value("${harms.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
  private String allowedOrigins;

  public WebMvcConfig(
      PermissionAuthorizationService permissionAuthorizationService,
      ActiveSessionService activeSessionService) {
    this.permissionAuthorizationService = permissionAuthorizationService;
    this.activeSessionService = activeSessionService;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    //拦截/api/** 校验登录session
    registry
        .addInterceptor(new SessionAuthenticationInterceptor(activeSessionService))
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/users/login", "/api/users/register", "/api/system/health", "/api/public/**");

    //要求医生角色
    registry
        .addInterceptor(new RoleAuthorizationInterceptor("DOCTOR"))
        .addPathPatterns("/api/doctor/**");

    //要求患者角色
    registry
        .addInterceptor(new RoleAuthorizationInterceptor("PATIENT"))
        .addPathPatterns("/api/patient/**");

    //要求管理员角色
    registry
        .addInterceptor(new RoleAuthorizationInterceptor("ADMIN"))
        .addPathPatterns("/api/admin/**");

    //要求挂号员角色
    registry
        .addInterceptor(new RoleAuthorizationInterceptor("PHARMACY"))
        .addPathPatterns("/api/pharmacy/**");

    //要求科室管理权限
    registry
        .addInterceptor(
            new PermissionAuthorizationInterceptor(
                permissionAuthorizationService, "DEPARTMENT_MANAGE"))
        .addPathPatterns("/api/departments/**");
  }

  /**
   * 配置某些前端网站可以调用接口
   */
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins(
            java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new))
        //允许的方法
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        //是否携带请求头
        .allowedHeaders("*")
        //是否允许携带cookie
        .allowCredentials(true)
        .maxAge(3600);
  }
}
