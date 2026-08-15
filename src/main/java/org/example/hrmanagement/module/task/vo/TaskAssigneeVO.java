package org.example.hrmanagement.module.task.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TaskAssigneeVO {

    private Long employeeId;

    private String employeeName;

    /** 状态：0-待接收 1-进行中 2-已完成 3-已驳回 4-已关闭 */
    private Integer status;

    /** 进度 0-100 */
    private Integer progress;

    private String feedback;

    private LocalDateTime acceptTime;

    private LocalDateTime finishTime;

    /** 1优 2良 3中 4合格 5差 */
    private Integer scoreGrade;

    private String scoreGradeLabel;

    private BigDecimal scoreBonus;

    private Long scoredBy;

    private String scoredByName;

    private LocalDateTime scoredAt;
}
