package org.example.hrmanagement.module.leave.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LeaveTypeCreateDTO {
    @NotBlank(message = "类型名称不能为空")
    private String typeName;
    @NotBlank(message = "类型编码不能为空")
    private String typeCode;

    /** 每年最大天数，NULL表示不限 */
    private Integer maxDays;

    /** 状态：0-禁用 1-启用 */
    private Integer status;
}
