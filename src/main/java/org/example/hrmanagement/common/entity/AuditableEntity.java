package org.example.hrmanagement.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 可审计实体基类（无软删除）。
 * 继承 BaseEntity 但排除 deleted 字段，适用于 Attendance、LeaveRequest 等物理删除的表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AuditableEntity extends BaseEntity {

    /** 排除父类的 deleted 逻辑删除字段，因为本实体对应的表没有该列 */
    @TableField(exist = false)
    private Integer deleted;

}
