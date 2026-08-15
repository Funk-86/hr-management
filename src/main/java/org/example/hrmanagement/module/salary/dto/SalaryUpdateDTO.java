package org.example.hrmanagement.module.salary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalaryUpdateDTO {
    @NotNull(message = "员工id不为空")
    private Long employeeId;
    @NotBlank(message = "薪资月份不为空")
    private String salaryMonth;
    @NotNull(message = "基础工资不为空")
    private BigDecimal baseSalary;

    /** 奖金 */
    private BigDecimal bonus;

    /** 扣款 */
    private BigDecimal deduction;
    private String remark;
}
