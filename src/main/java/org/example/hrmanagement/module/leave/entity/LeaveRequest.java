package org.example.hrmanagement.module.leave.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.AuditableEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_leave_request")
public class LeaveRequest extends AuditableEntity {

    /** 申请人ID */
    private Long employeeId;

    /** 假期类型ID */
    private Long leaveTypeId;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 请假天数 */
    private BigDecimal days;

    /** 请假原因 */
    private String reason;

    /** 状态：0-待审批 1-已通过 2-已拒绝 3-已撤销 */
    private Integer status;

    /** 审批人ID */
    private Long approverId;

    /** 审批时间 */
    private LocalDateTime approveTime;

    /** 审批备注 */
    private String approveRemark;
}
