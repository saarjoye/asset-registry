package com.company.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("employee")
public class Employee {
  @TableId(type = IdType.INPUT)
  private String id;
  private String employeeNo;
  private String name;
  private String gender;
  private Integer age;
  private String departmentId;
  private String positionId;
  private LocalDate hireDate;
  private String status;
  private String loginAccount;
  @JsonIgnore
  @TableField("login_password_hash")
  private String loginPasswordHash;
  private String roleCode;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public String getId() { return id; }
  public void setId(String v) { this.id = v; }
  public String getEmployeeNo() { return employeeNo; }
  public void setEmployeeNo(String v) { this.employeeNo = v; }
  public String getName() { return name; }
  public void setName(String v) { this.name = v; }
  public String getGender() { return gender; }
  public void setGender(String v) { this.gender = v; }
  public Integer getAge() { return age; }
  public void setAge(Integer v) { this.age = v; }
  public String getDepartmentId() { return departmentId; }
  public void setDepartmentId(String v) { this.departmentId = v; }
  public String getPositionId() { return positionId; }
  public void setPositionId(String v) { this.positionId = v; }
  public LocalDate getHireDate() { return hireDate; }
  public void setHireDate(LocalDate v) { this.hireDate = v; }
  public String getStatus() { return status; }
  public void setStatus(String v) { this.status = v; }
  public String getLoginAccount() { return loginAccount; }
  public void setLoginAccount(String v) { this.loginAccount = v; }
  public String getLoginPasswordHash() { return loginPasswordHash; }
  public void setLoginPasswordHash(String v) { this.loginPasswordHash = v; }
  public String getRoleCode() { return roleCode; }
  public void setRoleCode(String v) { this.roleCode = v; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
