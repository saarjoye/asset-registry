package com.company.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.asset.entity.Employee;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
}
