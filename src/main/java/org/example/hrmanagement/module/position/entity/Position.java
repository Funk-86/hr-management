package org.example.hrmanagement.module.position.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_position")
public class Position extends BaseEntity {

    /** 岗位名称 */
    private String positionName;

    /** 岗位编码 */
    private String positionCode;

    /** 所属部门ID */
    private Long deptId;

    /** 职级：1-普通 2-主管 3-经理 4-总监 */
    private Integer level;

    /** 状态：0-禁用 1-启用 */
    private Integer status;
}
