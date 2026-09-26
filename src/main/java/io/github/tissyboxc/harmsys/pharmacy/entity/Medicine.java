package io.github.tissyboxc.harmsys.pharmacy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** medicine 表实体。 */
@TableName("medicine")
public class Medicine {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String medicineCode;
  private String name;
  private String specification;
  private String unit;
  private BigDecimal unitPrice;
  private BigDecimal stockQuantity;
  private BigDecimal warningQuantity;
  private String manufacturer;
  private Integer status;
  private String remark;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getMedicineCode() {
    return medicineCode;
  }

  public void setMedicineCode(String medicineCode) {
    this.medicineCode = medicineCode;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSpecification() {
    return specification;
  }

  public void setSpecification(String specification) {
    this.specification = specification;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice;
  }

  public BigDecimal getStockQuantity() {
    return stockQuantity;
  }

  public void setStockQuantity(BigDecimal stockQuantity) {
    this.stockQuantity = stockQuantity;
  }

  public BigDecimal getWarningQuantity() {
    return warningQuantity;
  }

  public void setWarningQuantity(BigDecimal warningQuantity) {
    this.warningQuantity = warningQuantity;
  }

  public String getManufacturer() {
    return manufacturer;
  }

  public void setManufacturer(String manufacturer) {
    this.manufacturer = manufacturer;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
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
