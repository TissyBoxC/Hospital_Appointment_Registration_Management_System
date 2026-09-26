package io.github.tissyboxc.harmsys.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** payment_record 表实体。 */
@TableName("payment_record")
public class PaymentRecord {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String paymentNo;
  private Long appointmentId;
  private Long patientId;
  private BigDecimal amount;
  private Integer paymentMethod;
  private Integer status;
  private String thirdPartyNo;
  private LocalDateTime paidAt;
  private LocalDateTime refundedAt;
  private String refundReason;
  private Long refundOperatorId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPaymentNo() {
    return paymentNo;
  }

  public void setPaymentNo(String paymentNo) {
    this.paymentNo = paymentNo;
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

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public Integer getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(Integer paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public String getThirdPartyNo() {
    return thirdPartyNo;
  }

  public void setThirdPartyNo(String thirdPartyNo) {
    this.thirdPartyNo = thirdPartyNo;
  }

  public LocalDateTime getPaidAt() {
    return paidAt;
  }

  public void setPaidAt(LocalDateTime paidAt) {
    this.paidAt = paidAt;
  }

  public LocalDateTime getRefundedAt() {
    return refundedAt;
  }

  public void setRefundedAt(LocalDateTime refundedAt) {
    this.refundedAt = refundedAt;
  }

  public String getRefundReason() {
    return refundReason;
  }

  public void setRefundReason(String refundReason) {
    this.refundReason = refundReason;
  }

  public Long getRefundOperatorId() {
    return refundOperatorId;
  }

  public void setRefundOperatorId(Long refundOperatorId) {
    this.refundOperatorId = refundOperatorId;
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
