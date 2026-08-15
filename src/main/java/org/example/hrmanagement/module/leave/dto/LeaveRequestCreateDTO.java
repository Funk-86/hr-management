package org.example.hrmanagement.module.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LeaveRequestCreateDTO {

    @NotNull(message = "假期类型不能为空")
    /** 假期类型ID */
    private Long leaveTypeId;

    /** 开始时间 */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /** 结束时间 */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @NotNull(message = "请假天数不能为空")
    /** 请假天数 */
    private BigDecimal days;

    @NotBlank(message = "请假原因不能为空")
    /** 请假原因 */
    private String reason;

    /** 状态：0-待审批 1-已通过 2-已拒绝 3-已撤销 */
    private Integer status;

}
