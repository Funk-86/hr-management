package org.example.hrmanagement.module.task.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskScoreDTO {

    /** 1优 2良 3中 4合格 5差 */
    @NotNull(message = "请选择评分等级")
    @Min(1)
    @Max(5)
    private Integer grade;
}
