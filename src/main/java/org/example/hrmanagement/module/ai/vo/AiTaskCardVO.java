package org.example.hrmanagement.module.ai.vo;

import lombok.Data;

@Data
public class AiTaskCardVO {
    private Long id;
    private String title;
    private String myStatusLabel;
    private Integer myProgress;
    private String priorityLabel;
    private String dueTime;
    private Boolean overdue;
    private String creatorName;
}
