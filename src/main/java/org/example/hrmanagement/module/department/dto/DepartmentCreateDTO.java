package org.example.hrmanagement.module.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DepartmentCreateDTO {
    @NotNull
    private Long parentId;      // 0 表示根节点

    @NotBlank
    private String deptName;

    @NotBlank
    private String deptCode;

    private Long leaderId;      // 可选，员工模块做完后再关联
    private Integer sortOrder;  // 可选，默认 0
    private Integer status;     // 可选，默认 1
}