package org.example.hrmanagement.module.salary.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SalaryVO {
    private Long id;
    private Long employeeId;
    private String empNo;
    private String employeeName;
    private String salaryMonth;
    private Long positionId;
    private String positionName;
    private BigDecimal baseSalary;
    /** 任务评分奖金汇总 */
    private BigDecimal taskBonus;
    private BigDecimal bonus;
    private BigDecimal deduction;
    private BigDecimal actualSalary;
    private Integer status;
    private LocalDate payDate;
    private String remark;
}
