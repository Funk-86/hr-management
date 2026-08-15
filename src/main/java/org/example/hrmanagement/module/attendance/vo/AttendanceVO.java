package org.example.hrmanagement.module.attendance.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AttendanceVO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String empNo;
    private LocalDate attendDate;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private Integer status;
    private BigDecimal workHours;
    private String remark;
}
