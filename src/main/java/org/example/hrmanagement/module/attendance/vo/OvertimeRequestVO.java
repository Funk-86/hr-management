package org.example.hrmanagement.module.attendance.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class OvertimeRequestVO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private LocalDate workDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal hours;
    private String reason;
    private Integer status;
    private LocalDateTime approveTime;
    private String approveRemark;
}
