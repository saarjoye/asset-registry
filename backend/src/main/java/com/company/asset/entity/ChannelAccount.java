package com.company.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("channel_account")
public class ChannelAccount {
  @TableId(type = IdType.INPUT)
  private String id;
  private String employeeId;
  private String phoneId;
  private String channel;
  @TableField("account_name")
  private String accountName;
  @JsonIgnore
  @TableField("account_password_cipher")
  private String accountPasswordCipher;
  @TableField("real_name_status")
  private String realNameStatus;
  @TableField("real_name")
  private String realName;
  @JsonIgnore
  @TableField("id_card_cipher")
  private String idCardCipher;
  private String status;
  private LocalDate registeredAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public String getId() { return id; }
  public void setId(String v) { this.id = v; }
  public String getEmployeeId() { return employeeId; }
  public void setEmployeeId(String v) { this.employeeId = v; }
  public String getPhoneId() { return phoneId; }
  public void setPhoneId(String v) { this.phoneId = v; }
  public String getChannel() { return channel; }
  public void setChannel(String v) { this.channel = v; }
  public String getAccountName() { return accountName; }
  public void setAccountName(String v) { this.accountName = v; }
  public String getAccountPasswordCipher() { return accountPasswordCipher; }
  public void setAccountPasswordCipher(String v) { this.accountPasswordCipher = v; }
  public String getRealNameStatus() { return realNameStatus; }
  public void setRealNameStatus(String v) { this.realNameStatus = v; }
  public String getRealName() { return realName; }
  public void setRealName(String v) { this.realName = v; }
  public String getIdCardCipher() { return idCardCipher; }
  public void setIdCardCipher(String v) { this.idCardCipher = v; }
  public String getStatus() { return status; }
  public void setStatus(String v) { this.status = v; }
  public LocalDate getRegisteredAt() { return registeredAt; }
  public void setRegisteredAt(LocalDate v) { this.registeredAt = v; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
