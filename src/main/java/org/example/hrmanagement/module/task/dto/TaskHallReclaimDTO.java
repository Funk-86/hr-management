package org.example.hrmanagement.module.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskHallReclaimDTO {

    @NotBlank(message = "请填写收回原因")
    @Size(max = 255, message = "原因过长")
    private String reason;

    /** BACK_TO_HALL / CLOSE；收回单人时忽略，默认释放名额 */
    private String action;

    /** 有值则只收回该执行人；为空则整单 */
    private Long employeeId;
}
