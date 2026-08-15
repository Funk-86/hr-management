package org.example.hrmanagement.module.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProjectCreateDTO {

    @NotBlank(message = "项目名称不能为空")
    private String name;

    private String description;

    /** 负责人员工ID，默认当前登录人 */
    private Long ownerId;

    private Long deptId;

    /** 0规划 1进行中，默认 1 */
    private Integer status;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 初始成员（不含负责人也会自动加入） */
    private List<Long> memberIds;
}
