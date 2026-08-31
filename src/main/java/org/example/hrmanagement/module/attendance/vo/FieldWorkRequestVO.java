package org.example.hrmanagement.module.attendance.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FieldWorkRequestVO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private LocalDate workDate;
    private String location;
    private String reason;
    private Integer status;
    private LocalDateTime approveTime;
    private String approveRemark;
}
