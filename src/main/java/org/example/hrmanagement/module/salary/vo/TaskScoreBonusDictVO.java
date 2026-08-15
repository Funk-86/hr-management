package org.example.hrmanagement.module.salary.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TaskScoreBonusDictVO {
    private Long id;
    private Integer grade;
    private String gradeLabel;
    private BigDecimal bonusAmount;
    private Integer status;
}
