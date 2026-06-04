package com.company.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("phone_number")
public class PhoneNumber {
  @TableId(type = IdType.INPUT)
  private String id;
  private String employeeId;
  private String phoneNumber;
  private String operator;
  private String status;
  private LocalDate registeredAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public String getId() { return id; }
  public void setId(String v) { this.id = v; }
  public String getEmployeeId() { return employeeId; }
  public void setEmployeeId(String v) { this.employeeId = v; }
  public String getPhoneNumber() { return phoneNumber; }
  public void setPhoneNumber(String v) { this.phoneNumber = v; }
  public String getOperator() { return operator; }
  public void setOperator(String v) { this.operator = v; }
  public String getStatus() { return status; }
  public void setStatus(String v) { this.status = v; }
  public LocalDate getRegisteredAt() { return registeredAt; }
  public void setRegisteredAt(LocalDate v) { this.registeredAt = v; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
