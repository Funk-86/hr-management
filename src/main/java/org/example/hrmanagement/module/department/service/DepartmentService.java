package org.example.hrmanagement.module.department.service;

import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.department.dto.DepartmentCreateDTO;
import org.example.hrmanagement.module.department.dto.DepartmentUpdateDTO;
import org.example.hrmanagement.module.department.entity.Department;
import org.example.hrmanagement.module.department.vo.DepartmentTreeVO;

import java.util.List;

public interface DepartmentService {
    void createDepartment(DepartmentCreateDTO dto);

    void updateDepartment(Long id,DepartmentUpdateDTO updateDTO);

    void deleteDepartment(Long id);

    //列表树
    List<DepartmentTreeVO> listTree();

    //详情（单个）
    DepartmentTreeVO getById(Long id);

}
