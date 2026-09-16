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

  @PostMapping("/patients")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminCreateAccountResult createPatient(
      @Valid @RequestBody AdminCreatePatientRequest request, HttpServletRequest httpRequest) {
    return service.createPatient(request, httpRequest);
  }

  @PostMapping("/doctors")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminCreateAccountResult createDoctor(
      @Valid @RequestBody AdminCreateDoctorRequest request, HttpServletRequest httpRequest) {
    return service.createDoctor(request, httpRequest);
  }

  @PostMapping("/registrations")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminCreateAccountResult createRegistration(
      @Valid @RequestBody AdminCreateRegistrationRequest request, HttpServletRequest httpRequest) {
    return service.createRegistration(request, httpRequest);
  }

  @PostMapping({"/pharmacy", "/pharmacies"})
  @ResponseStatus(HttpStatus.CREATED)
  public AdminCreateAccountResult createPharmacy(
      @Valid @RequestBody AdminCreatePharmacyRequest request, HttpServletRequest httpRequest) {
    return service.createPharmacy(request, httpRequest);
  }

  @PutMapping("/users/{userId}/status")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateStatus(
      @PathVariable long userId,
      @Valid @RequestBody StatusRequest request,
      HttpServletRequest httpRequest) {
    service.updateStatus(userId, request.status(), httpRequest);
  }

  @PutMapping("/users/{userId}/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resetPassword(
      @PathVariable long userId,
      @Valid @RequestBody PasswordResetRequest request,
      HttpServletRequest httpRequest) {
    service.resetPassword(userId, request.password(), httpRequest);
  }

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
