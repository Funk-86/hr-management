package org.example.hrmanagement.module.task.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskDetailVO {

    private Long id;

    private String title;

    private String content;

    /** 父任务ID，0 表示根任务 */
    private Long parentId;

    /** 所属项目 */
    private Long projectId;

    /** 优先级：1-低 2-中 3-高 */
    private Integer priority;

    /** 整体状态：0-待接收 1-进行中 2-已完成 3-已关闭 */
    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime dueTime;

    private Long creatorId;

    /** 创建人姓名 */
    private String creatorName;

    /** 是否逾期 */
    private Boolean overdue;

    /** 当前登录人作为执行人的状态 */
    private Integer myStatus;

    /** 当前登录人作为执行人的进度 0-100 */
    private Integer myProgress;

    /** 任务整体进度 0-100（有子任务时为子任务进度平均，否则为执行人平均） */
    private Integer progress;

    /** 子任务列表（仅一层） */
    private List<TaskVO> children;

    /** 执行人列表 */
    private List<TaskAssigneeVO> assignees;

    /** 操作时间线 */
    private List<TaskLogVO> logs;

    /** 附件列表 */
    private List<TaskAttachmentVO> attachments;
}
