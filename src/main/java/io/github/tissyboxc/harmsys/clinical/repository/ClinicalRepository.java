package io.github.tissyboxc.harmsys.clinical.repository;

import io.github.tissyboxc.harmsys.clinical.dto.DiagnosisRequest;
import io.github.tissyboxc.harmsys.clinical.dto.PrescriptionItemRequest;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** 医生诊疗、诊断、处方与处方明细的数据访问层。 */
@Repository
public class ClinicalRepository {
  private final JdbcTemplate jdbc;

  public ClinicalRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> doctorAppointments(long doctorId) {
    return jdbc.queryForList(
        "SELECT a.*,p.real_name patient_name,p.phone patient_phone,dp.name department_name FROM"
            + " appointment a JOIN patient p ON p.id=a.patient_id JOIN department dp ON"
            + " dp.id=a.department_id WHERE a.doctor_id=? ORDER BY"
            + " a.appointment_date,a.queue_no,a.id",
        doctorId);
  }

  public List<Map<String, Object>> doctorAppointmentsPage(
      long doctorId, Integer status, int size, int offset) {
    String extra = status == null ? "" : " AND a.status=?";
    String sql =
        "SELECT a.*,p.real_name patient_name,p.phone patient_phone,dp.name department_name FROM"
            + " appointment a JOIN patient p ON p.id=a.patient_id JOIN department dp ON"
            + " dp.id=a.department_id WHERE a.doctor_id=?"
            + extra
            + " ORDER BY a.appointment_date,a.queue_no,a.id LIMIT ? OFFSET ?";
    return status == null
        ? jdbc.queryForList(sql, doctorId, size, offset)
        : jdbc.queryForList(sql, doctorId, status, size, offset);
  }

  public long doctorAppointmentsTotal(long doctorId, Integer status) {
    String extra = status == null ? "" : " AND a.status=?";
    return status == null
        ? count("SELECT COUNT(*) FROM appointment a WHERE a.doctor_id=?", doctorId)
        : count(
            "SELECT COUNT(*) FROM appointment a WHERE a.doctor_id=?" + extra, doctorId, status);
  }

  public Map<String, Object> doctorAppointment(long appointmentId, long doctorId) {
    return one(
        "SELECT a.*,p.real_name patient_name,p.phone patient_phone,p.gender"
            + " patient_gender,p.birthday patient_birthday,dp.name department_name FROM"
            + " appointment a JOIN patient p ON p.id=a.patient_id JOIN department dp ON"
            + " dp.id=a.department_id WHERE a.id=? AND a.doctor_id=?",
        appointmentId,
        doctorId);
  }

  public Map<String, Object> lockDoctorAppointment(long appointmentId, long doctorId) {
    return one(
        "SELECT * FROM appointment WHERE id=? AND doctor_id=? FOR UPDATE",
        appointmentId,
        doctorId);
  }

  public boolean departmentEnabled(long departmentId) {
    return count(
            "SELECT COUNT(*) FROM department WHERE id=? AND status=1 AND deleted=0",
            departmentId)
        > 0;
  }

  public int updatePatientName(long patientId, String realName) {
    return jdbc.update(
        "UPDATE patient SET real_name=? WHERE id=? AND deleted=0", realName, patientId);
  }

  public int updateAppointment(
      long appointmentId,
      Object appointmentDate,
      Object period,
      Object departmentId,
      Object queueNo,
      Object status,
      Object remark) {
    return jdbc.update(
        "UPDATE appointment SET"
            + " appointment_date=?,period=?,department_id=?,queue_no=?,status=?,remark=? WHERE"
            + " id=?",
        appointmentDate,
        period,
        departmentId,
        queueNo,
        status,
        remark,
        appointmentId);
  }

  public Map<String, Object> visitByAppointment(long appointmentId) {
    return one("SELECT * FROM medical_visit WHERE appointment_id=?", appointmentId);
  }

  public int insertVisit(long appointmentId, Object patientId, Object doctorId, String visitNo) {
    return jdbc.update(
        "INSERT INTO"
            + " medical_visit(appointment_id,patient_id,doctor_id,visit_no,visit_start_at,status)"
            + " VALUES(?,?,?,?,CURRENT_TIMESTAMP,2)",
        appointmentId,
        patientId,
        doctorId,
        visitNo);
  }

  public int startExistingVisit(Object visitId) {
    return jdbc.update(
        "UPDATE medical_visit SET"
            + " visit_start_at=COALESCE(visit_start_at,CURRENT_TIMESTAMP),status=2 WHERE id=?",
        visitId);
  }

  public int updateAppointmentStatus(long appointmentId, int status) {
    return jdbc.update("UPDATE appointment SET status=? WHERE id=?", status, appointmentId);
  }

  public int completeVisit(Object visitId) {
    return jdbc.update(
        "UPDATE medical_visit SET visit_end_at=CURRENT_TIMESTAMP,status=3 WHERE id=?", visitId);
  }

  public List<Map<String, Object>> doctorVisits(long doctorId) {
    return jdbc.queryForList(
        "SELECT v.*,p.real_name patient_name,a.appointment_no FROM medical_visit v JOIN patient p"
            + " ON p.id=v.patient_id JOIN appointment a ON a.id=v.appointment_id WHERE"
            + " v.doctor_id=? ORDER BY v.created_at DESC",
        doctorId);
  }

  public List<Map<String, Object>> patientVisits(long patientId) {
    return jdbc.queryForList(
        "SELECT v.*,d.real_name doctor_name,a.appointment_no FROM medical_visit v JOIN doctor d ON"
            + " d.id=v.doctor_id JOIN appointment a ON a.id=v.appointment_id WHERE v.patient_id=?"
            + " ORDER BY v.created_at DESC",
        patientId);
  }

  public Map<String, Object> visitDetail(long visitId) {
    return one(
        "SELECT v.*,p.real_name patient_name,d.real_name doctor_name,a.appointment_no FROM"
            + " medical_visit v JOIN patient p ON p.id=v.patient_id JOIN doctor d ON"
            + " d.id=v.doctor_id JOIN appointment a ON a.id=v.appointment_id WHERE v.id=?",
        visitId);
  }

  public Map<String, Object> doctorVisit(long visitId, long doctorId) {
    return one(
        "SELECT * FROM medical_visit WHERE id=? AND doctor_id=?", visitId, doctorId);
  }

  public int updateVisit(
      long visitId, String chiefComplaint, String presentIllness, String medicalAdvice) {
    return jdbc.update(
        "UPDATE medical_visit SET chief_complaint=?,present_illness=?,medical_advice=? WHERE id=?",
        chiefComplaint,
        presentIllness,
        medicalAdvice,
        visitId);
  }

  public List<Map<String, Object>> diagnoses(long visitId) {
    return jdbc.queryForList(
        "SELECT * FROM diagnosis_record WHERE visit_id=? ORDER BY id", visitId);
  }

  public long insertDiagnosis(long visitId, DiagnosisRequest request) {
    KeyHolder holder = new GeneratedKeyHolder();
    int rows =
        jdbc.update(
            connection -> {
              PreparedStatement statement =
                  connection.prepareStatement(
                      "INSERT INTO"
                          + " diagnosis_record(visit_id,diagnosis_name,diagnosis_code,diagnosis_type,remark)"
                          + " VALUES(?,?,?,?,?)",
                      Statement.RETURN_GENERATED_KEYS);
              statement.setLong(1, visitId);
              statement.setString(2, request.diagnosis_name());
              statement.setString(3, request.diagnosis_code());
              statement.setInt(4, request.diagnosis_type());
              statement.setString(5, request.remark());
              return statement;
            },
            holder);
    if (rows != 1 || holder.getKey() == null) throw new IllegalStateException("创建诊断失败");
    return holder.getKey().longValue();
  }

  public Map<String, Object> findDiagnosis(long id) {
    return one("SELECT * FROM diagnosis_record WHERE id=?", id);
  }

  public Map<String, Object> doctorDiagnosis(long id, long doctorId) {
    return one(
        "SELECT dr.* FROM diagnosis_record dr JOIN medical_visit v ON v.id=dr.visit_id WHERE"
            + " dr.id=? AND v.doctor_id=?",
        id,
        doctorId);
  }

  public int updateDiagnosis(long id, DiagnosisRequest request) {
    return jdbc.update(
        "UPDATE diagnosis_record SET diagnosis_name=?,diagnosis_code=?,diagnosis_type=?,remark=?"
            + " WHERE id=?",
        request.diagnosis_name(),
        request.diagnosis_code(),
        request.diagnosis_type(),
        request.remark(),
        id);
  }

  public int deleteDiagnosis(long id) {
    return jdbc.update("DELETE FROM diagnosis_record WHERE id=?", id);
  }

  public long insertPrescription(long visitId, String prescriptionNo, long doctorId) {
    KeyHolder holder = new GeneratedKeyHolder();
    int rows =
        jdbc.update(
            connection -> {
              PreparedStatement statement =
                  connection.prepareStatement(
                      "INSERT INTO prescription(visit_id,prescription_no,doctor_id,status)"
                          + " VALUES(?,?,?,1)",
                      Statement.RETURN_GENERATED_KEYS);
              statement.setLong(1, visitId);
              statement.setString(2, prescriptionNo);
              statement.setLong(3, doctorId);
              return statement;
            },
            holder);
    if (rows != 1 || holder.getKey() == null) throw new IllegalStateException("创建处方失败");
    return holder.getKey().longValue();
  }

  public Map<String, Object> prescription(long prescriptionId) {
    return one("SELECT * FROM prescription WHERE id=?", prescriptionId);
  }

  public Map<String, Object> visitOwner(long visitId) {
    return one("SELECT patient_id,doctor_id FROM medical_visit WHERE id=?", visitId);
  }

  public List<Map<String, Object>> prescriptionItems(long prescriptionId) {
    return jdbc.queryForList(
        "SELECT * FROM prescription_item WHERE prescription_id=? ORDER BY id", prescriptionId);
  }

  public Map<String, Object> lockDoctorPrescription(long prescriptionId, long doctorId) {
    return one(
        "SELECT * FROM prescription WHERE id=? AND doctor_id=? FOR UPDATE",
        prescriptionId,
        doctorId);
  }

  public BigDecimal prescriptionAmount(long prescriptionId) {
    BigDecimal amount =
        jdbc.queryForObject(
            "SELECT COALESCE(SUM(quantity*unit_price),0) FROM prescription_item WHERE prescription_id=?",
            BigDecimal.class,
            prescriptionId);
    return amount == null ? BigDecimal.ZERO : amount;
  }

  public int updatePrescriptionStatus(long prescriptionId, int status, BigDecimal amount) {
    if (status == 2)
      return jdbc.update(
          "UPDATE prescription SET status=2,total_amount=? WHERE id=?",
          amount,
          prescriptionId);
    return jdbc.update(
        "UPDATE prescription SET status=1,total_amount=0.00 WHERE id=? AND status=1",
        prescriptionId);
  }

  public Map<String, Object> lockPatientPrescription(long prescriptionId) {
    return one(
        "SELECT p.*,v.patient_id FROM prescription p JOIN medical_visit v ON v.id=p.visit_id"
            + " WHERE p.id=? FOR UPDATE",
        prescriptionId);
  }

  public int markPrescriptionPaid(
      long prescriptionId, String paymentNo, BigDecimal amount) {
    return jdbc.update(
        "UPDATE prescription SET payment_status=2,payment_no=?,total_amount=?,paid_at=CURRENT_TIMESTAMP"
            + " WHERE id=? AND payment_status=1",
        paymentNo,
        amount,
        prescriptionId);
  }

  public long insertPrescriptionItem(
      long prescriptionId, PrescriptionItemRequest request, Map<String, Object> medicine) {
    KeyHolder holder = new GeneratedKeyHolder();
    int rows =
        jdbc.update(
            connection -> {
              PreparedStatement statement =
                  connection.prepareStatement(
                      "INSERT INTO"
                          + " prescription_item(prescription_id,medicine_id,drug_name,specification,dosage,frequency,days,quantity,unit_price,remark)"
                          + " VALUES(?,?,?,?,?,?,?,?,?,?)",
                      Statement.RETURN_GENERATED_KEYS);
              statement.setLong(1, prescriptionId);
              statement.setLong(2, request.medicine_id());
              statement.setString(3, (String) medicine.get("name"));
              statement.setString(4, (String) medicine.get("specification"));
              statement.setString(5, request.dosage());
              statement.setString(6, request.frequency());
              statement.setInt(7, request.days());
              statement.setBigDecimal(8, request.quantity());
              statement.setBigDecimal(9, (BigDecimal) medicine.get("unit_price"));
              statement.setString(10, request.remark());
              return statement;
            },
            holder);
    if (rows != 1 || holder.getKey() == null) throw new IllegalStateException("添加处方明细失败");
    return holder.getKey().longValue();
  }

  public Map<String, Object> prescriptionItem(long id) {
    return one("SELECT * FROM prescription_item WHERE id=?", id);
  }

  public Map<String, Object> editablePrescriptionItem(long itemId, long doctorId) {
    return one(
        "SELECT i.id FROM prescription_item i JOIN prescription p ON p.id=i.prescription_id"
            + " WHERE i.id=? AND p.doctor_id=? AND p.status=1 AND p.payment_status=1",
        itemId,
        doctorId);
  }

  public int deletePrescriptionItem(long itemId) {
    return jdbc.update("DELETE FROM prescription_item WHERE id=?", itemId);
  }

  public int updatePrescriptionItem(
      long itemId, PrescriptionItemRequest request, Map<String, Object> medicine) {
    return jdbc.update(
        "UPDATE prescription_item SET"
            + " medicine_id=?,drug_name=?,specification=?,dosage=?,frequency=?,days=?,quantity=?,unit_price=?,remark=?"
            + " WHERE id=?",
        request.medicine_id(),
        medicine.get("name"),
        medicine.get("specification"),
        request.dosage(),
        request.frequency(),
        request.days(),
        request.quantity(),
        medicine.get("unit_price"),
        request.remark(),
        itemId);
  }

  public Map<String, Object> medicine(long medicineId) {
    return one(
        "SELECT id,name,specification,unit_price,stock_quantity FROM medicine WHERE id=? AND status=1",
        medicineId);
  }

  public void insertOperationLog(
      long userId, String type, String target, long id, String description, String ipAddress) {
    jdbc.update(
        "INSERT INTO"
            + " operation_log(user_id,operation_type,target_type,target_id,description,ip_address)"
            + " VALUES(?,?,?,?,?,?)",
        userId,
        type,
        target,
        id,
        description,
        ipAddress);
  }

  private long count(String sql, Object... args) {
    Long value = jdbc.queryForObject(sql, Long.class, args);
    return value == null ? 0 : value;
  }

  private Map<String, Object> one(String sql, Object... args) {
    return jdbc.query(sql, rs -> rs.next() ? row(rs) : null, args);
  }

  private Map<String, Object> row(ResultSet rs) throws SQLException {
    Map<String, Object> map = new LinkedHashMap<>();
    var metadata = rs.getMetaData();
    for (int i = 1; i <= metadata.getColumnCount(); i++)
      map.put(metadata.getColumnLabel(i), rs.getObject(i));
    return map;
  }
}
