package com.company.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("department")
public class Department {
  @TableId(type = IdType.INPUT)
  private String id;
  private String name;

  public Department() {
  }

  public Department(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
}
