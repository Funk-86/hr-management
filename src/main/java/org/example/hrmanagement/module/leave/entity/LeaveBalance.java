package org.example.hrmanagement.module.leave.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.AuditableEntity;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_leave_balance")
public class LeaveBalance extends AuditableEntity {

    private Long employeeId;

    private Long leaveTypeId;

    /** 年度 */
    private Integer year;

    /** 年度额度 */
    private BigDecimal quotaDays;

    /** 已通过占用 */
    private BigDecimal usedDays;

    /** 待审批占用 */
    private BigDecimal pendingDays;
}
