package org.example.hrmanagement.module.department.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_department")
public class Department extends BaseEntity {

    /** 父部门ID，0表示根节点 */
    private Long parentId;

    /** 部门名称 */
    private String deptName;

    /** 部门编码 */
    private String deptCode;

    /** 部门负责人（员工ID） */
    private Long leaderId;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：0-禁用 1-启用 */
    private Integer status;
}
