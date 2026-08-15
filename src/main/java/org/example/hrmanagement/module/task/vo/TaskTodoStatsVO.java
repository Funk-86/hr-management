package org.example.hrmanagement.module.task.vo;

import lombok.Data;

/**
 * 工作台：我的任务待办统计。
 */
@Data
public class TaskTodoStatsVO {

    /** 待接收 + 进行中数量 */
    private long todoCount;

    /** 逾期数量（待接收/进行中且已过截止时间） */
    private long overdueCount;
}
