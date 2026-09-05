package org.example.hrmanagement.module.employee.service;

import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.module.employee.dto.EmployeeCreateDTO;
import org.example.hrmanagement.module.employee.dto.EmployeeUpdateDTO;
import org.example.hrmanagement.module.employee.vo.EmployeeVO;

import java.util.List;

public interface EmployeeService {
    PageResult<EmployeeVO> list(PageQuery page, Long deptId, Integer status, String keyword);

    /** 导出用列表（复用数据权限与筛选，最多 5000 条） */
    List<EmployeeVO> listForExport(Long deptId, Integer status, String keyword);

    EmployeeVO findById(Long id);
    void insert(EmployeeCreateDTO dto);
    void update(Long id, EmployeeUpdateDTO dto);
    void delete(Long id);
}
