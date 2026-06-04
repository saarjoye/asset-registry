package com.company.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("handover_task")
public class HandoverTask {
  @TableId(type = IdType.INPUT)
  private String id;
  @TableField("applicant_id")
  private String applicantId;
  @TableField("source_department_id")
  private String sourceDepartmentId;
  @TableField("device_id")
  private String deviceId;
  @TableField("asset_type")
  private String assetType;
  @TableField("asset_id")
  private String assetId;
  @TableField("target_type")
  private String targetType;
  @TableField("receiver_employee_id")
  private String receiverEmployeeId;
  @TableField("receiver_department_id")
  private String receiverDepartmentId;
  @TableField("approved_by_id")
  private String approvedById;
  @TableField("approved_by_name")
  private String approvedByName;
  private String status;
  @TableField("applicant_note")
  private String applicantNote;
  @TableField("reject_reason")
  private String rejectReason;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public String getId() { return id; }
  public void setId(String v) { this.id = v; }
  public String getApplicantId() { return applicantId; }
  public void setApplicantId(String v) { this.applicantId = v; }
  public String getSourceDepartmentId() { return sourceDepartmentId; }
  public void setSourceDepartmentId(String v) { this.sourceDepartmentId = v; }
  public String getDeviceId() { return deviceId; }
  public void setDeviceId(String v) { this.deviceId = v; }
  public String getAssetType() { return assetType; }
  public void setAssetType(String v) { this.assetType = v; }
  public String getAssetId() { return assetId; }
  public void setAssetId(String v) { this.assetId = v; }
  public String getTargetType() { return targetType; }
  public void setTargetType(String v) { this.targetType = v; }
  public String getReceiverEmployeeId() { return receiverEmployeeId; }
  public void setReceiverEmployeeId(String v) { this.receiverEmployeeId = v; }
  public String getReceiverDepartmentId() { return receiverDepartmentId; }
  public void setReceiverDepartmentId(String v) { this.receiverDepartmentId = v; }
  public String getApprovedById() { return approvedById; }
  public void setApprovedById(String v) { this.approvedById = v; }
  public String getApprovedByName() { return approvedByName; }
  public void setApprovedByName(String v) { this.approvedByName = v; }
  public String getStatus() { return status; }
  public void setStatus(String v) { this.status = v; }
  public String getApplicantNote() { return applicantNote; }
  public void setApplicantNote(String v) { this.applicantNote = v; }
  public String getRejectReason() { return rejectReason; }
  public void setRejectReason(String v) { this.rejectReason = v; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
