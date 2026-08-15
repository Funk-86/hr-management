package org.example.hrmanagement.module.salary.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TaskScoreBonusDictSaveDTO {

    @NotNull(message = "等级不能为空")
    @Min(1)
    @Max(5)
    private Integer grade;

    @NotBlank(message = "等级名称不能为空")
    private String gradeLabel;

    @NotNull(message = "奖金不能为空")
    @DecimalMin(value = "0", message = "奖金不能为负")
    private BigDecimal bonusAmount;

    private Integer status;
}
