package org.example.hrmanagement.module.performance.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PerformanceReviewVO {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String empNo;
    private Long deptId;
    private String deptName;
    private Integer periodType;
    private String periodKey;
    private Integer scoreGrade;
    private String scoreGradeLabel;
    private String comment;
    private Integer taskDoneCount;
    private Integer taskTotalCount;
    private BigDecimal taskAvgGrade;
    private Long reviewerId;
    private String reviewerName;
    private Integer status;
    private String statusLabel;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
