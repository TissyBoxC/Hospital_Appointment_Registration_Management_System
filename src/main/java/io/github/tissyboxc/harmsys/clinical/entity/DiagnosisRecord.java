package io.github.tissyboxc.harmsys.clinical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** diagnosis_record 表实体。 */
@TableName("diagnosis_record")
public class DiagnosisRecord {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long visitId;
  private String diagnosisName;
  private String diagnosisCode;
  private Integer diagnosisType;
  private String remark;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getVisitId() {
    return visitId;
  }

  public void setVisitId(Long visitId) {
    this.visitId = visitId;
  }

  public String getDiagnosisName() {
    return diagnosisName;
  }

  public void setDiagnosisName(String diagnosisName) {
    this.diagnosisName = diagnosisName;
  }

  public String getDiagnosisCode() {
    return diagnosisCode;
  }

  public void setDiagnosisCode(String diagnosisCode) {
    this.diagnosisCode = diagnosisCode;
  }

  public Integer getDiagnosisType() {
    return diagnosisType;
  }

  public void setDiagnosisType(Integer diagnosisType) {
    this.diagnosisType = diagnosisType;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
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
