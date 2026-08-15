package org.example.hrmanagement.module.task.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskLogVO {

    /** CREATE / ACCEPT / PROGRESS / FINISH / REJECT / URGE / CLOSE */
    private String action;

    private String remark;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime createdAt;
}
