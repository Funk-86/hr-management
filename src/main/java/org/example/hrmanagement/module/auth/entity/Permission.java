package org.example.hrmanagement.module.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class Permission extends BaseEntity {

    /** 父权限ID，0表示根节点 */
    private Long parentId;

    /** 权限名称 */
    private String permName;

    /** 权限编码 */
    private String permCode;

    /** 类型：1-菜单 2-按钮 3-接口 */
    private Integer permType;

    /** 路由/接口路径 */
    private String path;

    /** HTTP方法 */
    private String method;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：0-禁用 1-启用 */
    private Integer status;
}
