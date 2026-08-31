package org.example.hrmanagement.module.salary.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AttendanceDeductRuleVO {
    private Long id;
    private String ruleCode;
    private BigDecimal unitAmount;
    private Integer enabled;
    private String remark;
}
