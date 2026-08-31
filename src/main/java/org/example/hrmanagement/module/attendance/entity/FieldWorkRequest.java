package org.example.hrmanagement.module.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.AuditableEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_field_work_request")
public class FieldWorkRequest extends AuditableEntity {
    private Long employeeId;
    private LocalDate workDate;
    private String location;
    private String reason;
    private Integer status;
    private Long approverId;
    private LocalDateTime approveTime;
    private String approveRemark;
}
