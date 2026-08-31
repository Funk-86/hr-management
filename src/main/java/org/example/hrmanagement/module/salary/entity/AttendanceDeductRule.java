package org.example.hrmanagement.module.salary.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.AuditableEntity;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_attendance_deduct_rule")
public class AttendanceDeductRule extends AuditableEntity {

    private String ruleCode;

    private BigDecimal unitAmount;

    /** 0禁用 1启用 */
    private Integer enabled;

    private String remark;
}
