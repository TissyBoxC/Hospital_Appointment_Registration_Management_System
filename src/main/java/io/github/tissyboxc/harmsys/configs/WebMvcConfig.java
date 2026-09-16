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
    registry
        .addInterceptor(new SessionAuthenticationInterceptor(activeSessionService))
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/users/login", "/api/users/register", "/api/system/health", "/api/public/**");

    registry
        .addInterceptor(new RoleAuthorizationInterceptor("DOCTOR"))
        .addPathPatterns("/api/doctor/**");

    registry
        .addInterceptor(new RoleAuthorizationInterceptor("PATIENT"))
        .addPathPatterns("/api/patient/**");

    registry
        .addInterceptor(new RoleAuthorizationInterceptor("ADMIN"))
        .addPathPatterns("/api/admin/**");

    registry
        .addInterceptor(new RoleAuthorizationInterceptor("PHARMACY"))
        .addPathPatterns("/api/pharmacy/**");

    registry
        .addInterceptor(
            new PermissionAuthorizationInterceptor(
                permissionAuthorizationService, "DEPARTMENT_MANAGE"))
        .addPathPatterns("/api/departments/**");
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins(
            java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new))
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
  }
}
