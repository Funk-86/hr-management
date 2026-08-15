package org.example.hrmanagement.module.performance.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PerformanceTaskHintVO {

    private Long employeeId;
    private Integer periodType;
    private String periodKey;
    private Integer taskDoneCount;
    private Integer taskTotalCount;
    /** 有评分的任务均分；无评分则为 null */
    private BigDecimal taskAvgGrade;
    private Integer scoredCount;
}
