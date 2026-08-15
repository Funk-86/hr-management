package org.example.hrmanagement.module.department.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DepartmentUpdateDTO {
    @NotBlank
    private String deptName;

    private Long leaderId;
    private Integer sortOrder;
    private Integer status;
    // parentId 是否允许改：第一版建议不改，避免整棵树乱掉
}