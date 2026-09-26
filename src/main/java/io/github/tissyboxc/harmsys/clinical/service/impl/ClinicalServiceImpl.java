package io.github.tissyboxc.harmsys.clinical.service.impl;

import io.github.tissyboxc.harmsys.clinical.service.ClinicalService;

import io.github.tissyboxc.harmsys.clinical.dto.*;
import io.github.tissyboxc.harmsys.clinical.entity.DiagnosisRecord;
import io.github.tissyboxc.harmsys.clinical.entity.Prescription;
import io.github.tissyboxc.harmsys.clinical.entity.PrescriptionItem;
import io.github.tissyboxc.harmsys.clinical.mapper.ClinicalMapper;
import io.github.tissyboxc.harmsys.operationlog.entity.OperationLog;
import io.github.tissyboxc.harmsys.operationlog.mapper.OperationLogMapper;
import io.github.tissyboxc.harmsys.security.SessionAuthenticationException;
import io.github.tissyboxc.harmsys.common.UserRegistrationException;
import io.github.tissyboxc.harmsys.security.session.AuthenticatedUser;
import io.github.tissyboxc.harmsys.security.session.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 负责就诊、诊断和处方状态流转的业务服务。 */
public class ClinicalServiceImpl implements ClinicalService {
  private final ClinicalMapper mapper;
  private final OperationLogMapper operationLogMapper;

  public ClinicalServiceImpl(ClinicalMapper mapper, OperationLogMapper operationLogMapper) {
    this.mapper = mapper;
    this.operationLogMapper = operationLogMapper;
  }

  /**
   * 查询该医生的被预约信息
   * @param r
   * @return
   */
  @Override
  public List<Map<String, Object>> doctorAppointments(HttpServletRequest r) {
    long doctor = requireDoctor(r).doctor_id();
    return mapper.selectDoctorAppointments(doctor);
  }

  /**
   * 医生分页查询患者信息
   */
  @Override
  public Map<String, Object> doctorAppointmentsPage(
      HttpServletRequest r, int page, int size, Integer status) {
    long doctor = requireDoctor(r).doctor_id();
    int pg = Math.max(1, page), s = Math.min(Math.max(1, size), 100), offset = (pg - 1) * s;
    List<Map<String, Object>> items =
        mapper.selectDoctorAppointmentsPage(doctor, status, s, offset);
    long total = mapper.countDoctorAppointments(doctor, status);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("page", pg);
    out.put("page_size", s);
    out.put("total", total);
    out.put("items", items);
    return out;
  }

  /**
   * 按ID查询预约信息
   */
  @Override
  public Map<String, Object> doctorAppointment(long id, HttpServletRequest r) {
    long doctor = requireDoctor(r).doctor_id();
    Map<String, Object> m = mapper.selectDoctorAppointment(id, doctor);
    if (m == null) throw new UserRegistrationException(404, "预约不存在或不属于当前医生");
    return m;
  }

  /**
   *医生修改患者预约信息
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> updateDoctorAppointment(
      long appointmentId, DoctorAppointmentUpdateRequest x, HttpServletRequest r) {
    //验证医生身份
    AuthenticatedUser doctor = requireDoctor(r);
    //获取当前预约信息
    Map<String, Object> appointment =
        mapper.lockDoctorAppointment(appointmentId, doctor.doctor_id());
    if (appointment == null) throw new UserRegistrationException(404, "预约不存在或不属于当前医生");
    //寻找科室
    if (mapper.countEnabledDepartment(x.department_id()) == 0)
      throw new UserRegistrationException(404, "科室不存在或已停用");
    //修改患者姓名
    long patientId = ((Number) appointment.get("patient_id")).longValue();
    if (mapper.updatePatientName(patientId, x.patient_name()) != 1)
      throw new UserRegistrationException(404, "患者资料不存在");
    //更新预约信息
    mapper.updateAppointment(
        appointmentId,
        x.appointment_date(),
        x.period(),
        x.department_id(),
        x.queue_no(),
        x.status(),
        x.remark());
    log(
        doctor.user_id(),
        "UPDATE_DOCTOR_APPOINTMENT",
        "appointment",
        appointmentId,
        "医生修改患者姓名及预约信息",
        r.getRemoteAddr());
    log(
        doctor.user_id(),
        "UPDATE_PATIENT_NAME",
        "patient",
        patientId,
        "医生修改预约患者姓名",
        r.getRemoteAddr());
    return doctorAppointment(appointmentId, r);
  }

  /**
   * 开始就诊逻辑
   * @param appointmentId 预约ID
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> startVisit(long appointmentId, HttpServletRequest r) {
    AuthenticatedUser doctor = requireDoctor(r);
    Map<String, Object> a =
        mapper.lockDoctorAppointment(appointmentId, doctor.doctor_id());
    if (a == null) throw new UserRegistrationException(404, "预约不存在或不属于当前医生");
    //校验是否签到
    if (((Number) a.get("status")).intValue() != 3)
      throw new UserRegistrationException(409, "只有已签到预约可以开始就诊");
    Map<String, Object> v = mapper.selectVisitByAppointment(appointmentId);
    //如果不存在就诊记录,则新建
    if (v == null) {
      String no =
          "V"
              + System.currentTimeMillis()
              + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
      mapper.insertVisit(appointmentId, a.get("patient_id"), a.get("doctor_id"), no);
    }
    //已有就诊记录:更新
    else {
      mapper.startExistingVisit(v.get("id"));
    }
    //更新预约状态
    mapper.updateAppointmentStatus(appointmentId, 4);
    log(
        doctor.user_id(),
        "START_VISIT",
        "medical_visit",
        appointmentId,
        "医生开始就诊",
        r.getRemoteAddr());
    return visitByAppointment(appointmentId);
  }

  /**
   * 完成就诊逻辑
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> completeVisit(long appointmentId, HttpServletRequest r) {
    AuthenticatedUser doctor = requireDoctor(r);
    Map<String, Object> a =
        mapper.lockDoctorAppointment(appointmentId, doctor.doctor_id());
    if (a == null) throw new UserRegistrationException(404, "预约不存在或不属于当前医生");
    if (((Number) a.get("status")).intValue() != 4)
      throw new UserRegistrationException(409, "只有就诊中的预约可以完成");
    Map<String, Object> v = mapper.selectVisitByAppointment(appointmentId);
    if (v == null) throw new UserRegistrationException(409, "就诊记录不存在");
    mapper.completeVisit(v.get("id"));
    mapper.updateAppointmentStatus(appointmentId, 5);
    log(
        doctor.user_id(),
        "COMPLETE_VISIT",
        "medical_visit",
        ((Number) v.get("id")).longValue(),
        "医生完成就诊",
        r.getRemoteAddr());
    return visitByAppointment(appointmentId);
  }

  /**
   * 医生查看自己的就诊记录列表
   */
  @Override
  public List<Map<String, Object>> doctorVisits(HttpServletRequest r) {
    long doctor = requireDoctor(r).doctor_id();
    return mapper.selectDoctorVisits(doctor);
  }

  /**
   * 患者查看自己的就诊记录列表
   */
  @Override
  public List<Map<String, Object>> patientVisits(HttpServletRequest r) {
    long patient = requirePatient(r).patient_id();
    return mapper.selectPatientVisits(patient);
  }

  /**
   * 查看某一条就诊记录信息
   * @param visit_id 就诊记录iD
   */
  @Override
  public Map<String, Object> getVisit(long visit_id, HttpServletRequest r) {
    AuthenticatedUser u = SessionAuth.require(r);
    Map<String, Object> v = mapper.selectVisitDetail(visit_id);
    if (v == null) throw new UserRegistrationException(404, "就诊记录不存在");
    if (!isAdmin(u)
        && !Objects.equals(u.doctor_id(), ((Number) v.get("doctor_id")).longValue())
        && !Objects.equals(u.patient_id(), ((Number) v.get("patient_id")).longValue()))
      throw new SessionAuthenticationException(403, "无权访问该就诊记录");
    return v;
  }

  /**
   * 医生修改某一条就诊记录的信息
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> updateVisit(long visit_id, VisitUpdateRequest x, HttpServletRequest r) {
    AuthenticatedUser d = requireDoctor(r);
    Map<String, Object> v = mapper.selectDoctorVisit(visit_id, d.doctor_id());
    if (v == null) throw new UserRegistrationException(404, "就诊记录不存在或不属于当前医生");
    if (((Number) v.get("status")).intValue() == 3)
      throw new UserRegistrationException(409, "已完成的就诊记录不能修改");
    mapper.updateVisit(
        visit_id, x.chief_complaint(), x.present_illness(), x.medical_advice());
    log(d.user_id(), "UPDATE_VISIT", "medical_visit", visit_id, "医生修改就诊记录", r.getRemoteAddr());
    return getVisit(visit_id, r);
  }

  /**
   * 查看某次就诊下的全部诊断
   */
  @Override
  public List<Map<String, Object>> diagnoses(long visitId, HttpServletRequest r) {
    doctorVisitOrPatient(visitId, r);
    return mapper.selectDiagnoses(visitId);
  }

  /**
   * 给某次就诊创建新的诊断
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> createDiagnosis(
      long visitId, DiagnosisRequest x, HttpServletRequest r) {
    AuthenticatedUser d = requireDoctor(r);
    if (mapper.selectDoctorVisit(visitId, d.doctor_id()) == null)
      throw new UserRegistrationException(404, "就诊记录不存在或不属于当前医生");
    DiagnosisRecord diagnosis = new DiagnosisRecord();
    diagnosis.setVisitId(visitId);
    diagnosis.setDiagnosisName(x.diagnosis_name());
    diagnosis.setDiagnosisCode(x.diagnosis_code());
    diagnosis.setDiagnosisType(x.diagnosis_type());
    diagnosis.setRemark(x.remark());
    mapper.insertDiagnosis(diagnosis);
    long diagnosisId = diagnosis.getId();
    log(
        d.user_id(),
        "CREATE_DIAGNOSIS",
        "diagnosis_record",
        diagnosisId,
        "医生创建诊断",
        r.getRemoteAddr());
    return mapper.selectDiagnosis(diagnosisId);
  }

  /**
   * 修改一条已有的诊断
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> updateDiagnosis(long id, DiagnosisRequest x, HttpServletRequest r) {
    AuthenticatedUser d = requireDoctor(r);
    Map<String, Object> m = mapper.selectDoctorDiagnosis(id, d.doctor_id());
    if (m == null) throw new UserRegistrationException(404, "诊断不存在或不属于当前医生");
    mapper.updateDiagnosis(
        id, x.diagnosis_name(), x.diagnosis_code(), x.diagnosis_type(), x.remark());
    log(d.user_id(), "UPDATE_DIAGNOSIS", "diagnosis_record", id, "医生修改诊断", r.getRemoteAddr());
    return mapper.selectDiagnosis(id);
  }

  /**
   * 删除诊断信息
   * @param diagnosis_id 诊断记录ID
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void deleteDiagnosis(long diagnosis_id, HttpServletRequest r) {
    AuthenticatedUser d = requireDoctor(r);
    if (mapper.selectDoctorDiagnosis(diagnosis_id, d.doctor_id()) == null)
      throw new UserRegistrationException(404, "诊断不存在或不属于当前医生");
    mapper.deleteDiagnosis(diagnosis_id);
    log(d.user_id(), "DELETE_DIAGNOSIS", "diagnosis_record", diagnosis_id, "医生删除诊断", r.getRemoteAddr());
  }

  /**
   * 创建处方
   * @param x 就诊记录
   * @param request 网络信息
   * @return 新建处方
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> createPrescription(PrescriptionRequest x, HttpServletRequest request) {
    AuthenticatedUser d = requireDoctor(request);
    if (mapper.selectDoctorVisit(x.visit_id(), d.doctor_id()) == null)
      throw new UserRegistrationException(404, "就诊记录不存在或不属于当前医生");
    String no =
        "RX"
            + System.currentTimeMillis()
            + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    Prescription prescription = new Prescription();
    prescription.setVisitId(x.visit_id());
    prescription.setPrescriptionNo(no);
    prescription.setDoctorId(d.doctor_id());
    mapper.insertPrescription(prescription);
    long prescriptionId = prescription.getId();
    log(
        d.user_id(),
        "CREATE_PRESCRIPTION",
        "prescription",
        prescriptionId,
        "医生创建处方",
        request.getRemoteAddr());
    return prescription(prescriptionId, request);
  }

  /**
   * 查询处方和处方明细
   * @param prescriptionid 处方ID
   * @return 处方和处方明细
   */
  @Override
  public Map<String, Object> prescription(long prescriptionid, HttpServletRequest request) {
    Map<String, Object> p = mapper.selectPrescription(prescriptionid);
    if (p == null) throw new UserRegistrationException(404, "处方不存在");
    AuthenticatedUser u = SessionAuth.require(request);
    Map<String, Object> v = mapper.selectVisitOwner(((Number) p.get("visit_id")).longValue());
    if (!isAdmin(u)
        && !Objects.equals(u.doctor_id(), ((Number) v.get("doctor_id")).longValue())
        && !Objects.equals(u.patient_id(), ((Number) v.get("patient_id")).longValue()))
      throw new SessionAuthenticationException(403, "无权访问该处方");
    p.put("items", mapper.selectPrescriptionItems(prescriptionid));
    return p;
  }

  /**
   * 修改处方状态
   * @param prescriptionId 处方ID
   * @param status 状态
   * @return 处方信息
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> updatePrescriptionStatus(long prescriptionId, int status, HttpServletRequest r) {
    AuthenticatedUser d = requireDoctor(r);
    Map<String, Object> p =
        mapper.lockDoctorPrescription(prescriptionId, d.doctor_id());
    if (p == null) throw new UserRegistrationException(404, "处方不存在或不属于当前医生");
    if (status < 1 || status > 2)
      throw new UserRegistrationException(422, "医生只能将处方设为草稿或已提交，已取药状态由药房设置");
    int paymentStatus = ((Number) p.get("payment_status")).intValue();
    if (paymentStatus != 1)
      throw new UserRegistrationException(409, "处方已支付或已退款，不能修改处方状态");
    if (((Number) p.get("status")).intValue() == 3)
      throw new UserRegistrationException(409, "已取药处方不能修改");
    if (((Number) p.get("status")).intValue() == 2 && status == 1)
      throw new UserRegistrationException(409, "已提交处方不能撤回为草稿");
    if (status == 2) {
      java.math.BigDecimal amount = mapper.selectPrescriptionAmount(prescriptionId);
      if (amount == null || amount.signum() <= 0)
        throw new UserRegistrationException(409, "处方没有有效明细，不能提交");
      mapper.submitPrescription(prescriptionId, amount);
    } else {
      mapper.resetPrescription(prescriptionId);
    }
    log(
        d.user_id(),
        "UPDATE_PRESCRIPTION_STATUS",
        "prescription",
            prescriptionId,
        "医生修改处方状态",
        r.getRemoteAddr());
    return prescription(prescriptionId, r);
  }

  /**
   * 患者支付处方。支付金额由后端按处方明细重新计算，不接受客户端金额。
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> payPrescription(long prescriptionId, HttpServletRequest request) {
    AuthenticatedUser patient = requirePatient(request);
    Map<String, Object> p = mapper.lockPatientPrescription(prescriptionId);
    if (p == null || !Objects.equals(patient.patient_id(), ((Number) p.get("patient_id")).longValue()))
      throw new UserRegistrationException(404, "处方不存在或不属于当前患者");
    int prescriptionStatus = ((Number) p.get("status")).intValue();
    int paymentStatus = ((Number) p.get("payment_status")).intValue();
    if (prescriptionStatus == 1)
      throw new UserRegistrationException(409, "处方尚未由医生提交，不能支付");
    if (prescriptionStatus == 3)
      throw new UserRegistrationException(409, "处方已取药，不能重复支付");
    if (paymentStatus == 2) return p;
    if (paymentStatus == 3) throw new UserRegistrationException(409, "已退款处方不能再次支付");

    java.math.BigDecimal amount = mapper.selectPrescriptionAmount(prescriptionId);
    if (amount == null || amount.signum() <= 0)
      throw new UserRegistrationException(409, "处方没有可支付的有效明细");
    String paymentNo =
        "RXP"
            + System.currentTimeMillis()
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    mapper.markPrescriptionPaid(prescriptionId, paymentNo, amount);
    log(
        patient.user_id(),
        "PAY_PRESCRIPTION",
        "prescription",
        prescriptionId,
        "患者完成处方模拟支付",
        request.getRemoteAddr());
    return mapper.selectPrescription(prescriptionId);
  }

  /**
   *添加新处方
   * @param prescriptionId 处方ID
   * @return 处方明细
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> addItem(
      long prescriptionId, PrescriptionItemRequest x, HttpServletRequest r) {
    AuthenticatedUser d = requireDoctor(r);
    Map<String, Object> p =
        mapper.lockDoctorPrescription(prescriptionId, d.doctor_id());
    if (p == null) throw new UserRegistrationException(404, "处方不存在或不属于当前医生");
    //验证处方状态和支付状态
    if (((Number) p.get("status")).intValue() != 1
        || ((Number) p.get("payment_status")).intValue() != 1)
      throw new UserRegistrationException(409, "只有未支付草稿处方可以添加明细");
    Map<String, Object> medicine = requireMedicine(x.medicine_id(), x.quantity());
    PrescriptionItem item = toPrescriptionItem(x, medicine);
    item.setPrescriptionId(prescriptionId);
    mapper.insertPrescriptionItem(item);
    long itemId = item.getId();
    log(
        d.user_id(),
        "CREATE_PRESCRIPTION_ITEM",
        "prescription_item",
        itemId,
        "医生添加处方明细",
        r.getRemoteAddr());
    return mapper.selectPrescriptionItem(itemId);
  }

  /**
   * 删除处方中物品
   * @param prescription_item_id 处方明细ID
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void deleteItem(long prescription_item_id, HttpServletRequest r) {
    AuthenticatedUser d = requireDoctor(r);
    if (mapper.selectEditablePrescriptionItem(prescription_item_id, d.doctor_id()) == null)
      throw new UserRegistrationException(404, "处方明细不存在、处方已提交或已支付，不能删除");
    mapper.deletePrescriptionItem(prescription_item_id);
    log(
        d.user_id(),
        "DELETE_PRESCRIPTION_ITEM",
        "prescription_item",
        prescription_item_id,
        "医生删除处方明细",
        r.getRemoteAddr());
  }

  /**
   *修改处方中物品
   * @return 新的处方明细
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public Map<String, Object> updateItem(long prescription_item_id, PrescriptionItemRequest x, HttpServletRequest request) {
    AuthenticatedUser d = requireDoctor(request);
    if (mapper.selectEditablePrescriptionItem(prescription_item_id, d.doctor_id()) == null)
      throw new UserRegistrationException(404, "处方明细不存在、处方已提交或已支付，不能修改");
    Map<String, Object> medicine = requireMedicine(x.medicine_id(), x.quantity());
    mapper.updatePrescriptionItem(
        prescription_item_id,
        x.medicine_id(),
        string(medicine, "name"),
        string(medicine, "specification"),
        x.dosage(),
        x.frequency(),
        x.days(),
        x.quantity(),
        decimal(medicine, "unit_price"),
        x.remark());
    log(
        d.user_id(),
        "UPDATE_PRESCRIPTION_ITEM",
        "prescription_item",
            prescription_item_id,
        "医生修改处方明细",
        request.getRemoteAddr());
    return mapper.selectPrescriptionItem(prescription_item_id);
  }

  private Map<String, Object> requireMedicine(long medicineId, java.math.BigDecimal requiredQuantity) {
    Map<String, Object> medicine = mapper.selectAvailableMedicine(medicineId);
    if (medicine == null) throw new UserRegistrationException(404, "药品不存在或已停用");
    java.math.BigDecimal stock = (java.math.BigDecimal) medicine.get("stock_quantity");
    if (stock == null || stock.signum() <= 0)
      throw new UserRegistrationException(409, "药品库存不足");
    if (requiredQuantity != null && stock.compareTo(requiredQuantity) < 0)
      throw new UserRegistrationException(
          409,
          "药品“"
              + medicine.get("name")
              + "”库存不足。现有 "
              + stock
              + "，需要 "
              + requiredQuantity);
    return medicine;
  }

  /**
   *根据预约ID查询就诊信息
   * @param appointment_id 预约ID
   * @return 就诊信息
   */
  private Map<String, Object> visitByAppointment(long appointment_id) {
    return mapper.selectVisitByAppointment(appointment_id);
  }

  /**
   *根据就诊记录ID查询就诊记录
   * @param medical_id 就诊号
   * @return 返回该患者的所有就诊信息
   */
  private Map<String, Object> doctorVisitOrPatient(long medical_id, HttpServletRequest request) {
    AuthenticatedUser u = SessionAuth.require(request);
    Map<String, Object> v = mapper.selectVisitDetail(medical_id);
    if (v == null
        || (!isAdmin(u)
            && !Objects.equals(u.doctor_id(), ((Number) v.get("doctor_id")).longValue())
            && !Objects.equals(u.patient_id(), ((Number) v.get("patient_id")).longValue())))
      throw new SessionAuthenticationException(403, "无权访问该就诊记录");
    return v;
  }

  /**
   * 日志记录
   */
  private void log(long uid, String type, String target, long id, String desc, String ip) {
    OperationLog log = new OperationLog();
    log.setUserId(uid);
    log.setOperationType(type);
    log.setTargetType(target);
    log.setTargetId(id);
    log.setDescription(desc);
    log.setIpAddress(ip);
    operationLogMapper.insert(log);
  }

  private PrescriptionItem toPrescriptionItem(
      PrescriptionItemRequest request, Map<String, Object> medicine) {
    PrescriptionItem item = new PrescriptionItem();
    item.setMedicineId(request.medicine_id());
    item.setDrugName(string(medicine, "name"));
    item.setSpecification(string(medicine, "specification"));
    item.setDosage(request.dosage());
    item.setFrequency(request.frequency());
    item.setDays(request.days());
    item.setQuantity(request.quantity());
    item.setUnitPrice(decimal(medicine, "unit_price"));
    item.setRemark(request.remark());
    return item;
  }

  private String string(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value == null ? null : String.valueOf(value);
  }

  private BigDecimal decimal(Map<String, Object> row, String key) {
    Object value = row.get(key);
    if (value == null) return null;
    return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
  }

  /**
   * 验证管理员身份
   */
  private boolean isAdmin(AuthenticatedUser u) {
    return u.role_codes().stream().anyMatch(x -> x.equalsIgnoreCase("ADMIN"));
  }

  /**
   * 验证医生身份
   */
  private AuthenticatedUser requireDoctor(HttpServletRequest request) {
    AuthenticatedUser u = SessionAuth.require(request);
    if (u.doctor_id() == null
        || u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("DOCTOR")))
      throw new SessionAuthenticationException(403, "当前账号不是医生");
    return u;
  }

  /**
   * 验证患者身份
   */
  private AuthenticatedUser requirePatient(HttpServletRequest request) {
    AuthenticatedUser u = SessionAuth.require(request);
    if (u.patient_id() == null
        || u.role_codes().stream().noneMatch(x -> x.equalsIgnoreCase("PATIENT")))
      throw new SessionAuthenticationException(403, "当前账号不是患者");
    return u;
  }
}

