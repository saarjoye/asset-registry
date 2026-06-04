package com.company.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("device_asset")
public class DeviceAsset {
  @TableId(type = IdType.INPUT)
  private String id;
  private String employeeId;
  private String departmentId;
  private String phoneId;
  @TableField("device_type")
  private String deviceType;
  private String brand;
  private String model;
  private String status;
  private LocalDate registeredAt;
  @TableField("recycle_initiator_employee_id")
  private String recycleInitiatorEmployeeId;
  @TableField("recycle_initiator_name")
  private String recycleInitiatorName;
  @TableField("recycle_source")
  private String recycleSource;
  @TableField("recycle_reason")
  private String recycleReason;
  @TableField("recycle_created_at")
  private LocalDateTime recycleCreatedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public String getId() { return id; }
  public void setId(String v) { this.id = v; }
  public String getEmployeeId() { return employeeId; }
  public void setEmployeeId(String v) { this.employeeId = v; }
  public String getDepartmentId() { return departmentId; }
  public void setDepartmentId(String v) { this.departmentId = v; }
  public String getPhoneId() { return phoneId; }
  public void setPhoneId(String v) { this.phoneId = v; }
  public String getDeviceType() { return deviceType; }
  public void setDeviceType(String v) { this.deviceType = v; }
  public String getBrand() { return brand; }
  public void setBrand(String v) { this.brand = v; }
  public String getModel() { return model; }
  public void setModel(String v) { this.model = v; }
  public String getStatus() { return status; }
  public void setStatus(String v) { this.status = v; }
  public LocalDate getRegisteredAt() { return registeredAt; }
  public void setRegisteredAt(LocalDate v) { this.registeredAt = v; }
  public String getRecycleInitiatorEmployeeId() { return recycleInitiatorEmployeeId; }
  public void setRecycleInitiatorEmployeeId(String v) { this.recycleInitiatorEmployeeId = v; }
  public String getRecycleInitiatorName() { return recycleInitiatorName; }
  public void setRecycleInitiatorName(String v) { this.recycleInitiatorName = v; }
  public String getRecycleSource() { return recycleSource; }
  public void setRecycleSource(String v) { this.recycleSource = v; }
  public String getRecycleReason() { return recycleReason; }
  public void setRecycleReason(String v) { this.recycleReason = v; }
  public LocalDateTime getRecycleCreatedAt() { return recycleCreatedAt; }
  public void setRecycleCreatedAt(LocalDateTime v) { this.recycleCreatedAt = v; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
