package org.example.hrmanagement.module.salary.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalaryBaseDictSaveDTO {

    @NotNull(message = "岗位不能为空")
    private Long positionId;

    @NotNull(message = "基本工资不能为空")
    @DecimalMin(value = "0", message = "基本工资不能为负")
    private BigDecimal baseSalary;

    /** 1-启用 0-停用，默认启用 */
    private Integer status;

    private String remark;
}
