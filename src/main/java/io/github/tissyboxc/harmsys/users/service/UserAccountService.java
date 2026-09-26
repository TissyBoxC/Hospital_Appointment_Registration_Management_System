package io.github.tissyboxc.harmsys.users.service;

import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;

import io.github.tissyboxc.harmsys.users.dto.PatientRegisterRequest;
import io.github.tissyboxc.harmsys.users.dto.RegisterResult;

/** 处理患者自助注册及初始角色分配。 */
public interface UserAccountService {
public RegisterResult PatientReg(PatientRegisterRequest request);
}
