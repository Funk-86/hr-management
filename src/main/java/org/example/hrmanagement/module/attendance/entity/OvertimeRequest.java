package org.example.hrmanagement.module.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.AuditableEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_overtime_request")
public class OvertimeRequest extends AuditableEntity {
    private Long employeeId;
    private LocalDate workDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal hours;
    private String reason;
    private Integer status;
    private Long approverId;
    private LocalDateTime approveTime;
    private String approveRemark;
}
