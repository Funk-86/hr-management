package org.example.hrmanagement.module.task.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskVO {

    private Long id;

    private String title;

    private String content;

    /** 父任务ID，0/空 表示根任务 */
    private Long parentId;

    /** 所属项目 */
    private Long projectId;

    /** 是否存在子任务 */
    private Boolean hasChildren;

    /** 优先级：1-低 2-中 3-高 */
    private Integer priority;

    /** 整体状态：0-待接收 1-进行中 2-已完成 3-已关闭 */
    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime dueTime;

    private Long creatorId;

    /** 创建人姓名 */
    private String creatorName;

    /** 当前用户作为执行人的状态：0-待接收 1-进行中 2-已完成 3-已驳回 4-已关闭 */
    private Integer myStatus;

    /** 当前用户作为执行人的进度 0-100 */
    private Integer myProgress;

    /**
     * 任务整体进度 0-100（所有执行人进度平均值，供列表进度条展示）。
     * 「我创建的」也会返回该字段。
     */
    private Integer progress;

    /** 是否逾期 */
    private Boolean overdue;
}
