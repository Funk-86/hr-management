package org.example.hrmanagement.module.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.annotation.OperationLog;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.auth.dto.RolePermissionSaveDTO;
import org.example.hrmanagement.module.auth.service.RolePermissionAdminService;
import org.example.hrmanagement.module.auth.vo.PermissionNodeVO;
import org.example.hrmanagement.module.auth.vo.RoleVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "字典-角色权限")
@RestController
@RequestMapping("/dict/rbac")
@RequiredArgsConstructor
public class RolePermissionAdminController {

    private final RolePermissionAdminService rolePermissionAdminService;

    @Operation(summary = "角色列表")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/roles")
    public Result<List<RoleVO>> roles() {
        return Result.success(rolePermissionAdminService.listRoles());
    }

    @Operation(summary = "权限树：1页面字典 2用户权限字典")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/permissions/tree")
    public Result<List<PermissionNodeVO>> tree(@RequestParam Integer permType) {
        return Result.success(rolePermissionAdminService.permissionTree(permType));
    }

    @Operation(summary = "某角色已勾选权限ID")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/roles/{roleCode}/permissions")
    public Result<List<Long>> checked(
            @PathVariable String roleCode,
            @RequestParam Integer permType) {
        return Result.success(rolePermissionAdminService.listCheckedPermissionIds(roleCode, permType));
    }

    @Operation(summary = "保存角色权限勾选（按类型覆盖）")
    @OperationLog(module = "字典管理", value = "保存角色权限")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/roles/{roleCode}/permissions")
    public Result<Void> save(
            @PathVariable String roleCode,
            @Valid @RequestBody RolePermissionSaveDTO dto) {
        rolePermissionAdminService.saveRolePermissions(roleCode, dto);
        return Result.success();
    }
}
