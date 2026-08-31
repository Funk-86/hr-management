package org.example.hrmanagement.module.salary.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalaryPreviewVO {
    private Long employeeId;
    private String empNo;
    private String employeeName;
    private String salaryMonth;
    private Long positionId;
    private String positionName;
    private BigDecimal baseSalary;
    private BigDecimal taskBonus;
    private BigDecimal bonus;
    private BigDecimal deduction;
    private BigDecimal actualSalary;
    /** 提示：如未配置底薪字典 */
    private String tip;
    /** 自动扣款明细说明 */
    private String deductDetail;
}
