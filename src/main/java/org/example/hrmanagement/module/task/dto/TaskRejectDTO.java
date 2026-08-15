package org.example.hrmanagement.module.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskRejectDTO {

    @NotBlank(message = "驳回原因不能为空")
    private String reason;
}
