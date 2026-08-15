package org.example.hrmanagement.module.salary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SalaryGenerateDTO {

    @NotNull(message = "员工不能为空")
    private Long employeeId;

    /** 格式 yyyy-MM */
    @NotBlank(message = "薪资月份不能为空")
    private String salaryMonth;
}
