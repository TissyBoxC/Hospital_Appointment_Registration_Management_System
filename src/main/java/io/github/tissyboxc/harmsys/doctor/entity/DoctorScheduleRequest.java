package io.github.tissyboxc.harmsys.doctor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** doctor_schedule_request 表实体。 */
@TableName("doctor_schedule_request")
public class DoctorScheduleRequest {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long doctorId;
  private Long departmentId;
  private Long targetScheduleId;
  private Integer requestType;
  private LocalDate scheduleDate;
  private Integer period;
  private LocalTime startTime;
  private LocalTime endTime;
  private Integer totalCount;
  private BigDecimal fee;
  private String remark;
  private Integer status;
  private Long requestedByUserId;
  private Long reviewedByUserId;
  private LocalDateTime reviewedAt;
  private String reviewRemark;
  private Long appliedScheduleId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getDoctorId() {
    return doctorId;
  }

  public void setDoctorId(Long doctorId) {
    this.doctorId = doctorId;
  }

  public Long getDepartmentId() {
    return departmentId;
  }

  public void setDepartmentId(Long departmentId) {
    this.departmentId = departmentId;
  }

  public Long getTargetScheduleId() {
    return targetScheduleId;
  }

  public void setTargetScheduleId(Long targetScheduleId) {
    this.targetScheduleId = targetScheduleId;
  }

  public Integer getRequestType() {
    return requestType;
  }

  public void setRequestType(Integer requestType) {
    this.requestType = requestType;
  }

  public LocalDate getScheduleDate() {
    return scheduleDate;
  }

  public void setScheduleDate(LocalDate scheduleDate) {
    this.scheduleDate = scheduleDate;
  }

  public Integer getPeriod() {
    return period;
  }

  public void setPeriod(Integer period) {
    this.period = period;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalTime endTime) {
    this.endTime = endTime;
  }

  public Integer getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(Integer totalCount) {
    this.totalCount = totalCount;
  }

  public BigDecimal getFee() {
    return fee;
  }

  public void setFee(BigDecimal fee) {
    this.fee = fee;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public Long getRequestedByUserId() {
    return requestedByUserId;
  }

  public void setRequestedByUserId(Long requestedByUserId) {
    this.requestedByUserId = requestedByUserId;
  }

  public Long getReviewedByUserId() {
    return reviewedByUserId;
  }

  public void setReviewedByUserId(Long reviewedByUserId) {
    this.reviewedByUserId = reviewedByUserId;
  }

  public LocalDateTime getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(LocalDateTime reviewedAt) {
    this.reviewedAt = reviewedAt;
  }

  public String getReviewRemark() {
    return reviewRemark;
  }

  public void setReviewRemark(String reviewRemark) {
    this.reviewRemark = reviewRemark;
  }

  public Long getAppliedScheduleId() {
    return appliedScheduleId;
  }

  public void setAppliedScheduleId(Long appliedScheduleId) {
    this.appliedScheduleId = appliedScheduleId;
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
