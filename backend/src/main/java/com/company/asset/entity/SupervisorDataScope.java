package com.company.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("supervisor_data_scope")
public class SupervisorDataScope {
  @TableId(type = IdType.INPUT)
  private String id;
  private String supervisorId;
  private String departmentId;
  private String positionId;
  private Boolean allPositions;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public String getId() { return id; }
  public void setId(String v) { this.id = v; }
  public String getSupervisorId() { return supervisorId; }
  public void setSupervisorId(String v) { this.supervisorId = v; }
  public String getDepartmentId() { return departmentId; }
  public void setDepartmentId(String v) { this.departmentId = v; }
  public String getPositionId() { return positionId; }
  public void setPositionId(String v) { this.positionId = v; }
  public Boolean getAllPositions() { return allPositions; }
  public void setAllPositions(Boolean v) { this.allPositions = v; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
