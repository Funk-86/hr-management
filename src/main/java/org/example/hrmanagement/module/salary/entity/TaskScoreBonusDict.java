package org.example.hrmanagement.module.salary.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.BaseEntity;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_task_score_bonus_dict")
public class TaskScoreBonusDict extends BaseEntity {

    /** 1优 2良 3中 4合格 5差 */
    private Integer grade;
    private String gradeLabel;
    private BigDecimal bonusAmount;
    /** 1-启用 0-停用 */
    private Integer status;
}
