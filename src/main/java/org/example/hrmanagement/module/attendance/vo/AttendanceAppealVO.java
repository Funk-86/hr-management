package org.example.hrmanagement.module.attendance.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class AttendanceAppealVO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private LocalDate attendDate;
    private Long attendanceId;
    private Integer fromStatus;
    private Integer toStatus;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private String reason;
    private Integer status;
    private LocalDateTime approveTime;
    private String approveRemark;
}
