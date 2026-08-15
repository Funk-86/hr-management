package org.example.hrmanagement.module.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskCreateDto {

    @NotBlank(message = "任务标题不能为空")
    private String title;

    private String content;

    /** 父任务ID，不传则为根任务（仅允许挂一层） */
    private Long parentId;

    /** 所属项目ID，可选；挂父任务时继承父任务项目 */
    private Long projectId;

    /** 优先级：1-低 2-中 3-高，默认中 */
    private Integer priority;

    private LocalDateTime startTime;

    private LocalDateTime dueTime;

    @NotEmpty(message = "请选择执行人")
    private List<Long> assigneeIds;
}
