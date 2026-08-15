package org.example.hrmanagement.module.performance.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_performance_review")
public class PerformanceReview extends BaseEntity {

    private Long employeeId;
    /** 1月度 2季度 */
    private Integer periodType;
    private String periodKey;
    /** 1优 2良 3中 4合格 5差 */
    private Integer scoreGrade;
    @TableField("comment_text")
    private String comment;
    private Integer taskDoneCount;
    private Integer taskTotalCount;
    private BigDecimal taskAvgGrade;
    private Long reviewerId;
    /** 0草稿 1已提交 2已确认 */
    private Integer status;
    private LocalDateTime confirmedAt;
    private Long confirmedBy;
}
