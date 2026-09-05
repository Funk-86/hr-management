package org.example.hrmanagement.module.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskHallAbandonDTO {

    @NotBlank(message = "请填写放弃原因")
    @Size(max = 255, message = "原因过长")
    private String reason;
}
