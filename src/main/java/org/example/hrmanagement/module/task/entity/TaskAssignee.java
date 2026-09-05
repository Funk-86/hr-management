package org.example.hrmanagement.module.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_task_assignee")
public class TaskAssignee extends BaseEntity {

    /** 任务ID */
    private Long taskId;

    /** 执行人（员工ID） */
    private Long employeeId;

    /** ASSIGN 指派 / CLAIM 大厅抢单 */
    private String source;

    /** 状态：0-待接收 1-进行中 2-已完成 3-已驳回 4-已关闭 */
    private Integer status;

    /** 进度 0-100 */
    private Integer progress;

    /** 进度/完成说明 */
    private String feedback;

    /** 驳回原因 */
    private String rejectReason;

    /** 接收时间 */
    private LocalDateTime acceptTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 评分等级：1优 2良 3中 4合格 5差 */
    private Integer scoreGrade;

    /** 评分奖金 */
    private BigDecimal scoreBonus;

    /** 评分人（员工ID） */
    private Long scoredBy;

    /** 评分时间 */
    private LocalDateTime scoredAt;
}
