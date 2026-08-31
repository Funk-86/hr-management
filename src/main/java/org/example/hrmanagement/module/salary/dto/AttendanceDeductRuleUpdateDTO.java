package org.example.hrmanagement.module.salary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AttendanceDeductRuleUpdateDTO {
    @NotBlank
    private String ruleCode;

    @NotNull
    private BigDecimal unitAmount;

    @NotNull
    private Integer enabled;

    private String remark;
}
