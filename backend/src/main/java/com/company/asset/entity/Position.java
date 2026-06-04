package com.company.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("position_table")
public class Position {
  @TableId(type = IdType.INPUT)
  private String id;
  @TableField("department_id")
  private String departmentId;
  private String name;

  public Position() {
  }

  public Position(String id, String departmentId, String name) {
    this.id = id;
    this.departmentId = departmentId;
    this.name = name;
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getDepartmentId() { return departmentId; }
  public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
}
