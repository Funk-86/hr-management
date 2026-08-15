package org.example.hrmanagement.module.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProjectUpdateDTO {

    @NotBlank(message = "项目名称不能为空")
    private String name;

    private String description;

    private Long ownerId;

    private Long deptId;

    /** 0规划 1进行中 2已完成 3已关闭 */
    private Integer status;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 全量覆盖成员列表（负责人始终保留） */
    private List<Long> memberIds;
}
