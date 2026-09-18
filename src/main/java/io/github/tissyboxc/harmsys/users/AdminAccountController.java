package io.github.tissyboxc.harmsys.users;

import io.github.tissyboxc.harmsys.users.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
/** 管理员创建患者、医生、挂号员和药房账号的接口。 */
public class AdminAccountController {
  private final AdminAccountService service;

  public AdminAccountController(AdminAccountService service) {
    this.service = service;
  }

  /**
   * 管理员创建患者账户
   */
  @PostMapping("/patients")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminCreateAccountResult createPatient(
      @Valid @RequestBody AdminCreatePatientRequest request, HttpServletRequest httpRequest) {
    return service.createPatient(request, httpRequest);
  }

  /**
   * 管理员创建医生账户
   */
  @PostMapping("/doctors")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminCreateAccountResult createDoctor(
      @Valid @RequestBody AdminCreateDoctorRequest request, HttpServletRequest httpRequest) {
    return service.createDoctor(request, httpRequest);
  }

  /**
   * 管理员创建挂号员账户
   */
  @PostMapping("/registrations")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminCreateAccountResult createRegistration(
      @Valid @RequestBody AdminCreateRegistrationRequest request, HttpServletRequest httpRequest) {
    return service.createRegistration(request, httpRequest);
  }

  /**
   * 管理员创建药房账户
   */
  @PostMapping({"/pharmacy", "/pharmacies"})
  @ResponseStatus(HttpStatus.CREATED)
  public AdminCreateAccountResult createPharmacy(
      @Valid @RequestBody AdminCreatePharmacyRequest request, HttpServletRequest httpRequest) {
    return service.createPharmacy(request, httpRequest);
  }

  /**
   * 管理员修改用户状态
   * @param userId 用户ID
   * @param request 包含状态的请求体
   */
  @PutMapping("/users/{userId}/status")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateStatus(
      @PathVariable long userId,
      @Valid @RequestBody StatusRequest request,
      HttpServletRequest httpRequest) {
    service.updateStatus(userId, request.status(), httpRequest);
  }

  /**
   * 管理员修改用户密码
   * @param userId 用户ID
   * @param request 包含新密码的请求体
   */
  @PutMapping("/users/{userId}/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resetPassword(
      @PathVariable long userId,
      @Valid @RequestBody PasswordResetRequest request,
      HttpServletRequest httpRequest) {
    service.resetPassword(userId, request.password(), httpRequest);
  }

  /**
   * 管理员任命科室负责人
   * @param userId 用户ID
   * @param request 角色启用状态
   */
  @PutMapping("/users/{userId}/department-manager")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setDepartmentManager(
      @PathVariable long userId,
      @Valid @RequestBody DepartmentManagerRequest request,
      HttpServletRequest httpRequest) {
    service.setDepartmentManager(userId, request.enabled(), httpRequest);
  }

  /** 账号启用或禁用请求。 */
  public record StatusRequest(@NotNull @Min(0) @Max(1) Integer status) {}

  /** 管理员重置账号密码的请求。 */
  public record PasswordResetRequest(@NotBlank @Size(min = 8, max = 64) String password) {}

  /** 科室负责人权限开关请求。 */
  public record DepartmentManagerRequest(@NotNull Boolean enabled) {}
}
