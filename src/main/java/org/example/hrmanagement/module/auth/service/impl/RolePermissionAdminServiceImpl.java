package org.example.hrmanagement.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.module.auth.dto.RolePermissionSaveDTO;
import org.example.hrmanagement.module.auth.entity.Permission;
import org.example.hrmanagement.module.auth.entity.Role;
import org.example.hrmanagement.module.auth.entity.RolePermission;
import org.example.hrmanagement.module.auth.mapper.PermissionMapper;
import org.example.hrmanagement.module.auth.mapper.RoleMapper;
import org.example.hrmanagement.module.auth.mapper.RolePermissionMapper;
import org.example.hrmanagement.module.auth.service.RolePermissionAdminService;
import org.example.hrmanagement.module.auth.vo.PermissionNodeVO;
import org.example.hrmanagement.module.auth.vo.RoleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolePermissionAdminServiceImpl implements RolePermissionAdminService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    public List<RoleVO> listRoles() {
        return roleMapper.selectList(
                        new LambdaQueryWrapper<Role>()
                                .eq(Role::getStatus, 1)
                                .orderByAsc(Role::getId))
                .stream()
                .map(r -> {
                    RoleVO vo = new RoleVO();
                    vo.setId(r.getId());
                    vo.setRoleName(r.getRoleName());
                    vo.setRoleCode(r.getRoleCode());
                    vo.setDescription(r.getDescription());
                    vo.setStatus(r.getStatus());
                    return vo;
                })
                .toList();
    }

    @Override
    public List<PermissionNodeVO> permissionTree(Integer permType) {
        if (permType == null || (permType != 1 && permType != 2)) {
            throw new BusinessException("permType 应为 1(页面) 或 2(能力)");
        }
        List<Permission> list = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>()
                        .eq(Permission::getPermType, permType)
                        .eq(Permission::getStatus, 1)
                        .orderByAsc(Permission::getSortOrder)
                        .orderByAsc(Permission::getId));
        return buildTree(list);
    }

    @Override
    public List<Long> listCheckedPermissionIds(String roleCode, Integer permType) {
        Role role = requireRole(roleCode);
        if (permType == null || (permType != 1 && permType != 2)) {
            throw new BusinessException("permType 应为 1(页面) 或 2(能力)");
        }
        Map<Long, Permission> all = permissionMapper.selectList(
                        new LambdaQueryWrapper<Permission>()
                                .eq(Permission::getPermType, permType)
                                .eq(Permission::getStatus, 1))
                .stream()
                .collect(Collectors.toMap(Permission::getId, p -> p, (a, b) -> a));
        if (all.isEmpty()) {
            return List.of();
        }
        // 原样返回库中勾选，不在回显时“假展开”子孙，避免掩盖未落库的叶子
        return rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermission>()
                                .eq(RolePermission::getRoleId, role.getId())
                                .in(RolePermission::getPermissionId, all.keySet()))
                .stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRolePermissions(String roleCode, RolePermissionSaveDTO dto) {
        Role role = requireRole(roleCode);
        Integer permType = dto.getPermType();
        if (permType == null || (permType != 1 && permType != 2)) {
            throw new BusinessException("permType 应为 1(页面) 或 2(能力)");
        }

        Set<Long> typeIds = permissionMapper.selectList(
                        new LambdaQueryWrapper<Permission>()
                                .eq(Permission::getPermType, permType)
                                .select(Permission::getId))
                .stream()
                .map(Permission::getId)
                .collect(Collectors.toSet());

        if (!typeIds.isEmpty()) {
            rolePermissionMapper.delete(
                    new LambdaQueryWrapper<RolePermission>()
                            .eq(RolePermission::getRoleId, role.getId())
                            .in(RolePermission::getPermissionId, typeIds));
        }

        Set<Long> incoming = new HashSet<>();
        if (dto.getPermissionIds() != null) {
            for (Long lid : dto.getPermissionIds()) {
                if (lid != null && typeIds.contains(lid)) {
                    incoming.add(lid);
                }
            }
        }

        // 页面/能力树：勾选父节点时展开全部子孙；勾选子节点时带上祖先
        if (!incoming.isEmpty()) {
            Map<Long, Permission> all = permissionMapper.selectList(
                            new LambdaQueryWrapper<Permission>().eq(Permission::getPermType, permType))
                    .stream()
                    .collect(Collectors.toMap(Permission::getId, p -> p, (a, b) -> a));
            incoming = expandWithDescendants(incoming, all);
            incoming = expandWithAncestors(incoming, all);
        }

        for (Long permId : incoming) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(role.getId());
            rp.setPermissionId(permId);
            rolePermissionMapper.insert(rp);
        }
    }

    /** 勾选父节点时，自动授予其下全部子孙权限 */
    private Set<Long> expandWithDescendants(Set<Long> ids, Map<Long, Permission> all) {
        Set<Long> result = new HashSet<>(ids);
        boolean grew;
        do {
            grew = false;
            for (Permission p : all.values()) {
                Long parentId = p.getParentId();
                if (parentId != null && parentId > 0 && result.contains(parentId) && result.add(p.getId())) {
                    grew = true;
                }
            }
        } while (grew);
        return result;
    }

    /** 勾选子节点时，自动带上祖先，保证树完整 */
    private Set<Long> expandWithAncestors(Set<Long> ids, Map<Long, Permission> all) {
        Set<Long> result = new HashSet<>(ids);
        for (Long id : ids) {
            Long pid = id;
            while (pid != null && all.containsKey(pid)) {
                result.add(pid);
                Long parentId = all.get(pid).getParentId();
                if (parentId == null || parentId == 0L || Objects.equals(parentId, pid)) {
                    break;
                }
                pid = parentId;
            }
        }
        return result;
    }

    private Role requireRole(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            throw new BusinessException("角色编码不能为空");
        }
        Role role = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, roleCode.trim()));
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }

    private List<PermissionNodeVO> buildTree(List<Permission> list) {
        Map<Long, PermissionNodeVO> map = new HashMap<>();
        for (Permission p : list) {
            PermissionNodeVO node = new PermissionNodeVO();
            node.setId(p.getId());
            node.setParentId(p.getParentId());
            node.setPermName(p.getPermName());
            node.setPermCode(p.getPermCode());
            node.setPermType(p.getPermType());
            node.setPath(p.getPath());
            node.setSortOrder(p.getSortOrder());
            map.put(p.getId(), node);
        }
        List<PermissionNodeVO> roots = new ArrayList<>();
        for (Permission p : list) {
            PermissionNodeVO node = map.get(p.getId());
            Long parentId = p.getParentId();
            if (parentId == null || parentId == 0L || !map.containsKey(parentId)) {
                roots.add(node);
            } else {
                map.get(parentId).getChildren().add(node);
            }
        }
        return roots;
    }
}
