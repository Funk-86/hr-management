package org.example.hrmanagement.module.task.vo;

import lombok.Data;

@Data
public class TaskHallClaimResultVO {
    private Long taskId;
    private Integer claimedCount;
    private Integer claimQuota;
    private Integer taskStatus;
}
