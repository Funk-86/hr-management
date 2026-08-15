package org.example.hrmanagement.module.department.vo;

import lombok.Data;

import java.util.List;

@Data
public class DepartmentTreeVO {
    private Long id;
    private Long parentId;
    private String deptName;
    private String deptCode;
    private Integer sortOrder;
    private Integer status;
    private List<DepartmentTreeVO> children;
}