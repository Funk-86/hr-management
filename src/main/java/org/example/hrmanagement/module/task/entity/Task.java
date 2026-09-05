package org.example.hrmanagement.module.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_task")
public class Task extends BaseEntity {

    private String title;
    private String content;
    /** 父任务ID，0 表示根任务 */
    private Long parentId;
    /** 所属项目ID，可空 */
    private Long projectId;
    /** 创建人（员工ID） */
    private Long creatorId;
    private Long deptId;
    /** 优先级：1-低 2-中 3-高 */
    private Integer priority;
    /** ASSIGNED 指派 / OPEN 大厅 */
    private String claimMode;
    /** 大厅名额：1 独占，N 协作 */
    private Integer claimQuota;
    /** 难度 1-5（大厅） */
    private Integer difficulty;
    /** 建议奖金（展示用） */
    private BigDecimal suggestBonus;
    /** MARK_ONLY / ZERO_BONUS / DEDUCT */
    private String overduePolicy;
    /** DEDUCT 时手填扣款额 */
    private BigDecimal deductAmount;
    /** 当前有效认领数缓存 */
    private Integer claimedCount;
    /** 乐观锁 */
    private Integer version;
    /** 整体状态：0-待接收/开放中 1-进行中 2-已完成 3-已关闭 */
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime dueTime;
}
