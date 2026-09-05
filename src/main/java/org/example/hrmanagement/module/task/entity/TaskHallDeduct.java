package org.example.hrmanagement.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("hr_task_hall_deduct")
public class TaskHallDeduct {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long employeeId;
    private BigDecimal amount;
    private String reason;
    private String deductMonth;
    private LocalDateTime createdAt;
    private Long createdBy;
}
