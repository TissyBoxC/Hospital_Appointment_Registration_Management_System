-- 1. 用户账号
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  username VARCHAR(50) NOT NULL COMMENT '登录名',
  password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt/Argon2 密码哈希',
  user_type TINYINT NOT NULL COMMENT '1患者 2医生 3管理员 4挂号员 5药房人员',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1正常',
  last_login_at DATETIME NULL COMMENT '最后登录时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_type_status (user_type, status),
  CONSTRAINT chk_sys_user_type CHECK (user_type IN (1,2,3,4,5)),
  CONSTRAINT chk_sys_user_status CHECK (status IN (0,1)),
  CONSTRAINT chk_sys_user_deleted CHECK (deleted IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='用户账号';

-- 2. 患者
CREATE TABLE IF NOT EXISTS patient (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT UNSIGNED NOT NULL COMMENT 'sys_user.id',
  real_name VARCHAR(50) NOT NULL COMMENT '姓名',
  id_card VARCHAR(32) NOT NULL COMMENT '身份证号，生产环境应加密或脱敏',
  gender TINYINT NOT NULL DEFAULT 0 COMMENT '0未知 1男 2女',
  birthday DATE NULL COMMENT '出生日期',
  phone VARCHAR(20) NOT NULL COMMENT '联系电话',
  address VARCHAR(255) NULL COMMENT '联系地址',
  emergency_contact VARCHAR(50) NULL COMMENT '紧急联系人',
  emergency_phone VARCHAR(20) NULL COMMENT '紧急联系电话',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_patient_user (user_id),
  UNIQUE KEY uk_patient_id_card (id_card),
  KEY idx_patient_phone (phone),
  CONSTRAINT fk_patient_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT chk_patient_gender CHECK (gender IN (0,1,2)),
  CONSTRAINT chk_patient_deleted CHECK (deleted IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='患者';

-- 3. 科室（parent_id 为 NULL 表示顶级科室）
CREATE TABLE IF NOT EXISTS department (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_id BIGINT UNSIGNED NULL COMMENT '父科室，顶级为 NULL',
  name VARCHAR(100) NOT NULL COMMENT '科室名称',
  code VARCHAR(50) NOT NULL COMMENT '科室编码',
  description VARCHAR(500) NULL COMMENT '科室介绍',
  location VARCHAR(255) NULL COMMENT '科室位置',
  contact_phone VARCHAR(20) NULL COMMENT '联系电话',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_department_code (code),
  UNIQUE KEY uk_department_parent_name (parent_id, name),
  KEY idx_department_parent (parent_id, status),
  CONSTRAINT fk_department_parent FOREIGN KEY (parent_id) REFERENCES department(id),
  CONSTRAINT chk_department_status CHECK (status IN (0,1)),
  CONSTRAINT chk_department_deleted CHECK (deleted IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='科室';

-- 4. 医生
CREATE TABLE IF NOT EXISTS doctor (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '医生登录账号',
  department_id BIGINT UNSIGNED NOT NULL COMMENT '所属科室',
  doctor_no VARCHAR(50) NOT NULL COMMENT '医生工号',
  real_name VARCHAR(50) NOT NULL COMMENT '医生姓名',
  title VARCHAR(50) NULL COMMENT '职称',
  specialty VARCHAR(500) NULL COMMENT '擅长领域',
  introduction TEXT NULL COMMENT '医生简介',
  avatar_url VARCHAR(500) NULL COMMENT '头像地址',
  consultation_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '默认挂号费',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0停诊/离职 1在职',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_doctor_user (user_id),
  UNIQUE KEY uk_doctor_no (doctor_no),
  KEY idx_doctor_department (department_id, status),
  CONSTRAINT fk_doctor_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_doctor_department FOREIGN KEY (department_id) REFERENCES department(id),
  CONSTRAINT chk_doctor_fee CHECK (consultation_fee >= 0),
  CONSTRAINT chk_doctor_status CHECK (status IN (0,1)),
  CONSTRAINT chk_doctor_deleted CHECK (deleted IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='医生';

-- 5. 医生排班和号源
CREATE TABLE IF NOT EXISTS doctor_schedule (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  doctor_id BIGINT UNSIGNED NOT NULL COMMENT '医生ID',
  department_id BIGINT UNSIGNED NOT NULL COMMENT '科室ID，冗余保存历史快照和查询性能',
  schedule_date DATE NOT NULL COMMENT '出诊日期',
  period TINYINT NOT NULL COMMENT '1上午 2下午 3晚上',
  start_time TIME NOT NULL COMMENT '开始时间',
  end_time TIME NOT NULL COMMENT '结束时间',
  total_count INT NOT NULL COMMENT '总号源数',
  booked_count INT NOT NULL DEFAULT 0 COMMENT '已预约数',
  fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '本次排班挂号费',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0未开始 1可预约 2停诊 3结束',
  remark VARCHAR(255) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_schedule_doctor_date_period (doctor_id, schedule_date, period),
  KEY idx_schedule_doctor_date (doctor_id, schedule_date, status),
  KEY idx_schedule_department_date (department_id, schedule_date, status),
  CONSTRAINT fk_schedule_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id),
  CONSTRAINT fk_schedule_department FOREIGN KEY (department_id) REFERENCES department(id),
  CONSTRAINT chk_schedule_period CHECK (period IN (1,2,3)),
  CONSTRAINT chk_schedule_count CHECK (total_count > 0 AND booked_count >= 0 AND booked_count <= total_count),
  CONSTRAINT chk_schedule_fee CHECK (fee >= 0),
  CONSTRAINT chk_schedule_status CHECK (status IN (0,1,2,3)),
  CONSTRAINT chk_schedule_time CHECK (start_time < end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='医生排班';

-- 6. 排班时间段
CREATE TABLE IF NOT EXISTS schedule_slot (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  schedule_id BIGINT UNSIGNED NOT NULL COMMENT '排班ID',
  slot_no INT NOT NULL COMMENT '排班内的序号',
  start_time TIME NOT NULL COMMENT '时间段开始',
  end_time TIME NOT NULL COMMENT '时间段结束',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0可预约 1已预约 2锁定',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_slot_schedule_no (schedule_id, slot_no),
  UNIQUE KEY uk_slot_schedule_time (schedule_id, start_time, end_time),
  KEY idx_slot_schedule_status (schedule_id, status),
  CONSTRAINT fk_slot_schedule FOREIGN KEY (schedule_id) REFERENCES doctor_schedule(id),
  CONSTRAINT chk_slot_no CHECK (slot_no > 0),
  CONSTRAINT chk_slot_status CHECK (status IN (0,1,2)),
  CONSTRAINT chk_slot_time CHECK (start_time < end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='排班时间段';

-- 7. 预约挂号
CREATE TABLE IF NOT EXISTS appointment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  appointment_no VARCHAR(40) NOT NULL COMMENT '预约单号',
  patient_id BIGINT UNSIGNED NOT NULL COMMENT '患者ID',
  doctor_id BIGINT UNSIGNED NOT NULL COMMENT '医生ID快照',
  department_id BIGINT UNSIGNED NOT NULL COMMENT '科室ID快照',
  schedule_id BIGINT UNSIGNED NOT NULL COMMENT '排班ID',
  slot_id BIGINT UNSIGNED NULL COMMENT '具体时间段ID；按整段号源时可为空',
  appointment_date DATE NOT NULL COMMENT '就诊日期快照',
  period TINYINT NOT NULL COMMENT '就诊时段快照：1上午 2下午 3晚上',
  queue_no INT NULL COMMENT '排队号',
  fee DECIMAL(10,2) NOT NULL COMMENT '下单时锁定的挂号费',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1待支付 2已预约 3已签到 4已就诊 5已完成 6患者取消 7医生停诊取消 8过期 9已退款',
  active_slot_id BIGINT UNSIGNED GENERATED ALWAYS AS
    (CASE WHEN status IN (1,2,3,4) THEN slot_id ELSE NULL END) STORED COMMENT '有效预约的时间段键',
  remark VARCHAR(500) NULL COMMENT '患者备注',
  cancel_reason VARCHAR(255) NULL COMMENT '取消原因',
  cancelled_at DATETIME NULL COMMENT '取消时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '预约创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_appointment_no (appointment_no),
  UNIQUE KEY uk_appointment_active_slot (schedule_id, active_slot_id),
  KEY idx_appointment_patient_status (patient_id, status, created_at),
  KEY idx_appointment_doctor_date (doctor_id, appointment_date, status),
  KEY idx_appointment_schedule (schedule_id, status),
  KEY idx_appointment_slot (slot_id, status),
  CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
  CONSTRAINT fk_appointment_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id),
  CONSTRAINT fk_appointment_department FOREIGN KEY (department_id) REFERENCES department(id),
  CONSTRAINT fk_appointment_schedule FOREIGN KEY (schedule_id) REFERENCES doctor_schedule(id),
  CONSTRAINT fk_appointment_slot FOREIGN KEY (slot_id) REFERENCES schedule_slot(id),
  CONSTRAINT chk_appointment_period CHECK (period IN (1,2,3)),
  CONSTRAINT chk_appointment_fee CHECK (fee >= 0),
  CONSTRAINT chk_appointment_status CHECK (status IN (1,2,3,4,5,6,7,8,9))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='预约挂号';

-- 8. 支付记录
CREATE TABLE IF NOT EXISTS payment_record (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  payment_no VARCHAR(64) NOT NULL COMMENT '系统支付单号',
  appointment_id BIGINT UNSIGNED NOT NULL COMMENT '预约ID',
  patient_id BIGINT UNSIGNED NOT NULL COMMENT '患者ID，便于对账',
  amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  payment_method TINYINT NOT NULL COMMENT '1现金 2微信 3支付宝 4银行卡',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1待支付 2成功 3失败 4退款中 5已退款',
  third_party_no VARCHAR(100) NULL COMMENT '第三方交易号',
  paid_at DATETIME NULL COMMENT '支付时间',
  refunded_at DATETIME NULL COMMENT '退款时间',
  refund_reason VARCHAR(255) NULL COMMENT '退款原因',
  refund_operator_id BIGINT UNSIGNED NULL COMMENT '退款操作人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_payment_no (payment_no),
  UNIQUE KEY uk_payment_third_party_no (third_party_no),
  KEY idx_payment_appointment (appointment_id, status),
  KEY idx_payment_patient (patient_id, created_at),
  CONSTRAINT fk_payment_appointment FOREIGN KEY (appointment_id) REFERENCES appointment(id),
  CONSTRAINT fk_payment_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
  CONSTRAINT fk_payment_refund_operator FOREIGN KEY (refund_operator_id) REFERENCES sys_user(id),
  CONSTRAINT chk_payment_amount CHECK (amount >= 0),
  CONSTRAINT chk_payment_method CHECK (payment_method IN (1,2,3,4)),
  CONSTRAINT chk_payment_status CHECK (status IN (1,2,3,4,5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='支付记录';

-- 9. 就诊记录
CREATE TABLE IF NOT EXISTS medical_visit (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  appointment_id BIGINT UNSIGNED NOT NULL COMMENT '对应预约',
  patient_id BIGINT UNSIGNED NOT NULL COMMENT '患者ID',
  doctor_id BIGINT UNSIGNED NOT NULL COMMENT '医生ID',
  visit_no VARCHAR(50) NOT NULL COMMENT '就诊编号',
  check_in_at DATETIME NULL COMMENT '签到时间',
  visit_start_at DATETIME NULL COMMENT '开始就诊时间',
  visit_end_at DATETIME NULL COMMENT '结束就诊时间',
  chief_complaint TEXT NULL COMMENT '主诉',
  present_illness TEXT NULL COMMENT '现病史',
  medical_advice TEXT NULL COMMENT '医嘱',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1待就诊 2就诊中 3已完成',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_visit_appointment (appointment_id),
  UNIQUE KEY uk_visit_no (visit_no),
  KEY idx_visit_patient (patient_id, created_at),
  KEY idx_visit_doctor (doctor_id, created_at),
  CONSTRAINT fk_visit_appointment FOREIGN KEY (appointment_id) REFERENCES appointment(id),
  CONSTRAINT fk_visit_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
  CONSTRAINT fk_visit_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id),
  CONSTRAINT chk_visit_status CHECK (status IN (1,2,3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='就诊记录';

-- 10. 诊断记录
CREATE TABLE IF NOT EXISTS diagnosis_record (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  visit_id BIGINT UNSIGNED NOT NULL COMMENT '就诊记录ID',
  diagnosis_name VARCHAR(255) NOT NULL COMMENT '诊断名称',
  diagnosis_code VARCHAR(50) NULL COMMENT '疾病编码',
  diagnosis_type TINYINT NOT NULL DEFAULT 1 COMMENT '1初步诊断 2确诊',
  remark VARCHAR(500) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_diagnosis_visit (visit_id),
  CONSTRAINT fk_diagnosis_visit FOREIGN KEY (visit_id) REFERENCES medical_visit(id),
  CONSTRAINT chk_diagnosis_type CHECK (diagnosis_type IN (1,2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='诊断记录';

-- 11. 处方
CREATE TABLE IF NOT EXISTS prescription (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  visit_id BIGINT UNSIGNED NOT NULL COMMENT '就诊记录ID',
  prescription_no VARCHAR(50) NOT NULL COMMENT '处方编号',
  doctor_id BIGINT UNSIGNED NOT NULL COMMENT '开方医生',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1草稿 2已提交 3已取药',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_prescription_no (prescription_no),
  KEY idx_prescription_visit (visit_id),
  CONSTRAINT fk_prescription_visit FOREIGN KEY (visit_id) REFERENCES medical_visit(id),
  CONSTRAINT fk_prescription_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id),
  CONSTRAINT chk_prescription_status CHECK (status IN (1,2,3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='处方';

-- 12. 处方明细
CREATE TABLE IF NOT EXISTS prescription_item (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  prescription_id BIGINT UNSIGNED NOT NULL COMMENT '处方ID',
  drug_name VARCHAR(255) NOT NULL COMMENT '药品名称',
  specification VARCHAR(100) NULL COMMENT '药品规格',
  dosage VARCHAR(100) NOT NULL COMMENT '单次用量',
  frequency VARCHAR(100) NOT NULL COMMENT '用药频率',
  days INT NOT NULL COMMENT '用药天数',
  quantity DECIMAL(10,2) NOT NULL COMMENT '开具数量',
  remark VARCHAR(255) NULL COMMENT '用药备注',
  PRIMARY KEY (id),
  KEY idx_prescription_item_prescription (prescription_id),
  CONSTRAINT fk_prescription_item_prescription FOREIGN KEY (prescription_id) REFERENCES prescription(id),
  CONSTRAINT chk_prescription_item_days CHECK (days > 0),
  CONSTRAINT chk_prescription_item_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='处方明细';

-- 13. 角色
CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
  role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code),
  CONSTRAINT chk_role_status CHECK (status IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='角色';

-- 14. 用户角色关联
CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='用户角色关联';

-- 15. 权限
CREATE TABLE IF NOT EXISTS sys_permission (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  permission_code VARCHAR(100) NOT NULL COMMENT '权限编码',
  permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
  type TINYINT NOT NULL COMMENT '1菜单 2按钮 3接口',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_permission_code (permission_code),
  CONSTRAINT chk_permission_type CHECK (type IN (1,2,3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='权限';

-- 16. 角色权限关联
CREATE TABLE IF NOT EXISTS sys_role_permission (
  role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  permission_id BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
  CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='角色权限关联';

-- 17. 操作日志
CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT UNSIGNED NULL COMMENT '操作人，系统任务可为空',
  operation_type VARCHAR(50) NOT NULL COMMENT '操作类型，如 CREATE_APPOINTMENT',
  target_type VARCHAR(50) NOT NULL COMMENT '目标类型，如 appointment',
  target_id BIGINT UNSIGNED NULL COMMENT '目标数据ID',
  description VARCHAR(500) NOT NULL COMMENT '操作描述',
  ip_address VARCHAR(50) NULL COMMENT '客户端IP',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (id),
  KEY idx_log_user_time (user_id, created_at),
  KEY idx_log_target (target_type, target_id),
  CONSTRAINT fk_operation_log_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='操作日志';

-- 基础角色与权限：使用幂等写入，应用每次启动执行 schema.sql 时不会产生重复数据
INSERT INTO sys_role (role_code, role_name, status) VALUES
('PATIENT', '患者', 1),
('DOCTOR', '医生', 1),
('ADMIN', '管理员', 1),
('REGISTRATION', '挂号员', 1),
('PHARMACY', '药房人员', 1),
('DEPARTMENT_MANAGER', '科室管理员', 1)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), status = 1;

INSERT INTO sys_permission (permission_code, permission_name, type) VALUES
('DEPARTMENT_MANAGE', '科室管理', 3),
('DOCTOR_MANAGE', '医生资料管理', 3),
('PATIENT_MANAGE', '患者资料管理', 3),
('SCHEDULE_SELF_MANAGE', '本人排班管理', 3),
('SCHEDULE_ALL_MANAGE', '全部排班管理', 3)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), type = VALUES(type);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code = 'ADMIN'
  AND p.permission_code IN ('DEPARTMENT_MANAGE','DOCTOR_MANAGE','PATIENT_MANAGE','SCHEDULE_SELF_MANAGE','SCHEDULE_ALL_MANAGE');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code = 'DEPARTMENT_MANAGER'
  AND p.permission_code = 'DEPARTMENT_MANAGE';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code = 'DOCTOR'
  AND p.permission_code = 'SCHEDULE_SELF_MANAGE';

-- 18. 预约幂等请求记录：防止客户端重试导致重复预约
CREATE TABLE IF NOT EXISTS appointment_idempotency (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  request_no VARCHAR(80) NOT NULL,
  patient_id BIGINT UNSIGNED NOT NULL,
  appointment_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_appointment_idempotency_request (request_no, patient_id),
  CONSTRAINT fk_idempotency_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
  CONSTRAINT fk_idempotency_appointment FOREIGN KEY (appointment_id) REFERENCES appointment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='预约幂等请求';

-- 19. 系统通知
CREATE TABLE IF NOT EXISTS system_notification (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  title VARCHAR(200) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  notification_type VARCHAR(50) NOT NULL,
  read_status TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  read_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_notification_user (user_id, read_status, created_at),
  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT chk_notification_read_status CHECK (read_status IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='系统通知';

-- 20. 系统参数
CREATE TABLE IF NOT EXISTS system_config (
  config_key VARCHAR(100) NOT NULL,
  config_value VARCHAR(500) NOT NULL,
  description VARCHAR(255) NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='系统参数';

-- 21. 数据库版本
CREATE TABLE IF NOT EXISTS database_schema_version (
  version INT NOT NULL,
  description VARCHAR(255) NOT NULL,
  applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='数据库版本';

INSERT INTO system_config(config_key, config_value, description) VALUES
('appointment.payment.mode', 'MOCK_AUTO_SUCCESS', '预约支付模式'),
('appointment.payment.timeout.minutes', '30', '待支付预约超时时间'),
('appointment.cancel.cutoff.minutes', '30', '预约开始前允许取消的分钟数'),
('appointment.expire.after.minutes', '60', '超过预约时间后自动过期分钟数')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), description = VALUES(description);

INSERT INTO database_schema_version(version, description) VALUES (1, '基础17张业务表及扩展表')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 22. 登录失败控制：限制连续失败登录，降低暴力尝试风险
CREATE TABLE IF NOT EXISTS login_attempt (
  username VARCHAR(50) NOT NULL,
  ip_address VARCHAR(50) NOT NULL,
  fail_count INT NOT NULL DEFAULT 0,
  locked_until DATETIME NULL,
  last_attempt_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (username, ip_address),
  CONSTRAINT chk_login_attempt_count CHECK (fail_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='登录失败控制';

INSERT INTO database_schema_version(version, description) VALUES (2, '退款审计、幂等、通知、系统参数和登录保护')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 23. 活跃会话：支持管理员禁用账号或重置密码后强制失效
CREATE TABLE IF NOT EXISTS active_session (
  session_id VARCHAR(128) NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at DATETIME NOT NULL,
  PRIMARY KEY (session_id),
  KEY idx_active_session_user (user_id),
  CONSTRAINT fk_active_session_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='活跃登录会话';

-- 24. 医疗附件、检查报告和检验结果
CREATE TABLE IF NOT EXISTS medical_attachment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  patient_id BIGINT UNSIGNED NOT NULL,
  visit_id BIGINT UNSIGNED NULL,
  uploader_user_id BIGINT UNSIGNED NOT NULL,
  attachment_type VARCHAR(50) NOT NULL COMMENT 'AVATAR/EXAM_REPORT/LAB_RESULT/MEDICAL_RECORD',
  original_name VARCHAR(255) NOT NULL,
  stored_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  content_type VARCHAR(100) NULL,
  file_size BIGINT NOT NULL DEFAULT 0,
  description VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_attachment_patient (patient_id, attachment_type, created_at),
  CONSTRAINT fk_attachment_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
  CONSTRAINT fk_attachment_visit FOREIGN KEY (visit_id) REFERENCES medical_visit(id),
  CONSTRAINT fk_attachment_uploader FOREIGN KEY (uploader_user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='医疗文件附件';

-- 25. 系统公告
CREATE TABLE IF NOT EXISTS system_announcement (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1发布 2撤回',
  publisher_user_id BIGINT UNSIGNED NOT NULL,
  published_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_announcement_status_time (status, published_at),
  CONSTRAINT fk_announcement_publisher FOREIGN KEY (publisher_user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='系统公告';

-- 26. 外部通知发送队列：当前为短信/邮件模拟接口
CREATE TABLE IF NOT EXISTS notification_outbox (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NULL,
  channel VARCHAR(20) NOT NULL COMMENT 'SMS/EMAIL',
  recipient VARCHAR(255) NOT NULL,
  subject VARCHAR(200) NULL,
  content VARCHAR(1000) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待发送 1模拟成功 2失败',
  error_message VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sent_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_outbox_status (status, created_at),
  CONSTRAINT fk_outbox_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='短信邮件发送队列';

INSERT INTO database_schema_version(version, description) VALUES (3, '活跃会话、医疗附件、公告和通知发送队列')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO database_schema_version(version, description) VALUES (4, '药房角色、药房处方流转和一致性维护')
ON DUPLICATE KEY UPDATE description = VALUES(description);
