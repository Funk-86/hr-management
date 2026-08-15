package org.example.hrmanagement.module.salary.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalaryBaseDictVO {
    private Long id;
    private Long positionId;
    private String positionName;
    private BigDecimal baseSalary;
    private Integer status;
    private String remark;
}
