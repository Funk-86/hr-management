package org.example.hrmanagement.module.task.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskProgressDTO {
    @NotNull
    @Min(0) @Max(100)
    private Integer progress;

    private String feedback;
}
