package org.example.hrmanagement.module.salary.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalaryMineExportRow {

    @ExcelProperty("月份")
    private String salaryMonth;

    @ExcelProperty("工号")
    private String empNo;

    @ExcelProperty("姓名")
    private String employeeName;

    @ExcelProperty("岗位")
    private String positionName;

    @ExcelProperty("底薪")
    private BigDecimal baseSalary;

    @ExcelProperty("任务奖金")
    private BigDecimal taskBonus;

    @ExcelProperty("最终奖金")
    private BigDecimal bonus;

    @ExcelProperty("扣款")
    private BigDecimal deduction;

    @ExcelProperty("实发")
    private BigDecimal actualSalary;

    @ExcelProperty("发放日")
    private String payDate;

    @ExcelProperty("备注")
    private String remark;
}
