package io.github.tissyboxc.harmsys.clinical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** medical_visit 表实体。 */
@TableName("medical_visit")
public class MedicalVisit {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long appointmentId;
  private Long patientId;
  private Long doctorId;
  private String visitNo;
  private String chiefComplaint;
  private String presentIllness;
  private String medicalAdvice;
  private LocalDateTime checkInAt;
  private LocalDateTime visitStartAt;
  private LocalDateTime visitEndAt;
  private Integer status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getAppointmentId() {
    return appointmentId;
  }

  public void setAppointmentId(Long appointmentId) {
    this.appointmentId = appointmentId;
  }

  public Long getPatientId() {
    return patientId;
  }

  public void setPatientId(Long patientId) {
    this.patientId = patientId;
  }

  public Long getDoctorId() {
    return doctorId;
  }

  public void setDoctorId(Long doctorId) {
    this.doctorId = doctorId;
  }

  public String getVisitNo() {
    return visitNo;
  }

  public void setVisitNo(String visitNo) {
    this.visitNo = visitNo;
  }

  public String getChiefComplaint() {
    return chiefComplaint;
  }

  public void setChiefComplaint(String chiefComplaint) {
    this.chiefComplaint = chiefComplaint;
  }

  public String getPresentIllness() {
    return presentIllness;
  }

  public void setPresentIllness(String presentIllness) {
    this.presentIllness = presentIllness;
  }

  public String getMedicalAdvice() {
    return medicalAdvice;
  }

  public void setMedicalAdvice(String medicalAdvice) {
    this.medicalAdvice = medicalAdvice;
  }

  public LocalDateTime getCheckInAt() {
    return checkInAt;
  }

  public void setCheckInAt(LocalDateTime checkInAt) {
    this.checkInAt = checkInAt;
  }

  public LocalDateTime getVisitStartAt() {
    return visitStartAt;
  }

  public void setVisitStartAt(LocalDateTime visitStartAt) {
    this.visitStartAt = visitStartAt;
  }

  public LocalDateTime getVisitEndAt() {
    return visitEndAt;
  }

  public void setVisitEndAt(LocalDateTime visitEndAt) {
    this.visitEndAt = visitEndAt;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
