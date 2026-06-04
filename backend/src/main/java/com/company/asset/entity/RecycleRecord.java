package com.company.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("recycle_record")
public class RecycleRecord {
  @TableId(type = IdType.INPUT)
  private String id;
  @TableField("device_id")
  private String deviceId;
  @TableField("former_employee_id")
  private String formerEmployeeId;
  @TableField("target_employee_id")
  private String targetEmployeeId;
  @TableField("department_id")
  private String departmentId;
  @TableField("action_type")
  private String actionType;
  @TableField("action_time")
  private LocalDateTime actionTime;
  @TableField("operator_employee_id")
  private String operatorEmployeeId;

  public String getId() { return id; }
  public void setId(String v) { this.id = v; }
  public String getDeviceId() { return deviceId; }
  public void setDeviceId(String v) { this.deviceId = v; }
  public String getFormerEmployeeId() { return formerEmployeeId; }
  public void setFormerEmployeeId(String v) { this.formerEmployeeId = v; }
  public String getTargetEmployeeId() { return targetEmployeeId; }
  public void setTargetEmployeeId(String v) { this.targetEmployeeId = v; }
  public String getDepartmentId() { return departmentId; }
  public void setDepartmentId(String v) { this.departmentId = v; }
  public String getActionType() { return actionType; }
  public void setActionType(String v) { this.actionType = v; }
  public LocalDateTime getActionTime() { return actionTime; }
  public void setActionTime(LocalDateTime v) { this.actionTime = v; }
  public String getOperatorEmployeeId() { return operatorEmployeeId; }
  public void setOperatorEmployeeId(String v) { this.operatorEmployeeId = v; }
}
