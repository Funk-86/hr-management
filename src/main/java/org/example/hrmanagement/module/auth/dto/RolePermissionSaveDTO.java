package org.example.hrmanagement.module.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RolePermissionSaveDTO {

    /** 1页面 2能力 */
    @NotNull(message = "权限类型不能为空")
    private Integer permType;

    /**
     * 勾选的权限ID（含父节点亦可；保存时会展开子孙并补祖先）。
     */
    private List<Long> permissionIds;
}
