package org.example.hrmanagement.module.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.example.hrmanagement.common.entity.BaseEntity;

import java.time.LocalDateTime;

import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee_face")
public class EmployeeFace extends BaseEntity {
    private Long employeeId;
    private String descriptor;
    private Integer sampleCount;
    private Long enrolledBy;
    private LocalDateTime enrolledAt;
    private String modelVersion;
    private Integer status;
}
