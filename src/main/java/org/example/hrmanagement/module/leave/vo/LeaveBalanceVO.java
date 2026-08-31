package org.example.hrmanagement.module.leave.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LeaveBalanceVO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long leaveTypeId;
    private String leaveTypeName;
    private String leaveTypeCode;
    private Integer year;
    private BigDecimal quotaDays;
    private BigDecimal usedDays;
    private BigDecimal pendingDays;
    /** 剩余 = quota - used - pending */
    private BigDecimal remainingDays;
}
