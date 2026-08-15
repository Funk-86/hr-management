package org.example.hrmanagement.module.auth.service;

import org.example.hrmanagement.module.auth.dto.RolePermissionSaveDTO;
import org.example.hrmanagement.module.auth.vo.PermissionNodeVO;
import org.example.hrmanagement.module.auth.vo.RoleVO;

import java.util.List;

public interface RolePermissionAdminService {

    List<RoleVO> listRoles();

    List<PermissionNodeVO> permissionTree(Integer permType);

    List<Long> listCheckedPermissionIds(String roleCode, Integer permType);

    void saveRolePermissions(String roleCode, RolePermissionSaveDTO dto);
}
