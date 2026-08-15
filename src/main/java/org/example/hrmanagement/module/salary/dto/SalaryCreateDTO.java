package org.example.hrmanagement.module.salary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalaryCreateDTO {
    @NotNull(message = "员工id不为空")
    private Long employeeId;
    @NotBlank(message = "薪资月份不为空")
    private String salaryMonth;
    @NotNull(message = "基础工资不为空")
    private BigDecimal baseSalary;
    /** 任务评分奖金汇总（可选，默认 0） */
    private BigDecimal taskBonus;
    private BigDecimal bonus;
    private BigDecimal deduction;
    private Long positionId;
    private String remark;
}
