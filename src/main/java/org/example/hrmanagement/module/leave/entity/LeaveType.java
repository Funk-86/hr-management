package org.example.hrmanagement.module.leave.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.AuditableEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_leave_type")
public class LeaveType extends AuditableEntity {

    /** 类型名称（年假/病假/事假等） */
    private String typeName;

    /** 类型编码 */
    private String typeCode;

    /** 每年最大天数，NULL表示不限 */
    private Integer maxDays;

    /** 状态：0-禁用 1-启用 */
    private Integer status;
}
