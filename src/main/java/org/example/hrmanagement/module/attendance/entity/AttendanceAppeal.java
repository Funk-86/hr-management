package org.example.hrmanagement.module.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.AuditableEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_attendance_appeal")
public class AttendanceAppeal extends AuditableEntity {
    private Long employeeId;
    private LocalDate attendDate;
    private Long attendanceId;
    private Integer fromStatus;
    private Integer toStatus;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private String reason;
    private Integer status;
    private Long approverId;
    private LocalDateTime approveTime;
    private String approveRemark;
}
