package org.example.hrmanagement.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiTaskDraftRequest {

    /** 任务关键词或简要需求 */
    @NotBlank(message = "请输入任务关键词")
    private String prompt;
}
