package org.example.hrmanagement.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.hrmanagement.module.auth.entity.Permission;

import java.util.List;

public interface PermissionMapper extends BaseMapper<Permission> {
    /** 查用户角色编码 */
    @Select("""
        SELECT DISTINCT r.role_code
        FROM sys_role r
        INNER JOIN sys_user_role ur ON r.id = ur.role_id
        WHERE ur.user_id = #{userId}
          AND r.deleted = 0
          AND r.status = 1
        """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /** 用户是否拥有指定角色 */
    @Select("""
        SELECT COUNT(1)
        FROM sys_role r
        INNER JOIN sys_user_role ur ON r.id = ur.role_id
        WHERE ur.user_id = #{userId}
          AND r.role_code = #{roleCode}
          AND r.deleted = 0
          AND r.status = 1
        """)
    int countUserRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    /** 查用户权限编码 */
    @Select("""
        SELECT DISTINCT p.perm_code
        FROM sys_permission p
        INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
        INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id
        WHERE ur.user_id = #{userId}
          AND p.deleted = 0
          AND p.status = 1
        """)
    List<String> selectPermCodesByUserId(@Param("userId") Long userId);

    /** 按用户 + 登录角色查权限编码 */
    @Select("""
        SELECT DISTINCT p.perm_code
        FROM sys_permission p
        INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
        INNER JOIN sys_role r ON r.id = rp.role_id
        INNER JOIN sys_user_role ur ON ur.role_id = r.id
        WHERE ur.user_id = #{userId}
          AND r.role_code = #{roleCode}
          AND p.deleted = 0
          AND p.status = 1
          AND r.deleted = 0
          AND r.status = 1
        """)
    List<String> selectPermCodesByUserIdAndRole(
            @Param("userId") Long userId, @Param("roleCode") String roleCode);

    /** 查用户菜单（perm_type = 1） */
    @Select("""
        SELECT DISTINCT p.id, p.parent_id, p.perm_name, p.perm_code,
               p.path, p.sort_order
        FROM sys_permission p
        INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
        INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id
        WHERE ur.user_id = #{userId}
          AND p.deleted = 0
          AND p.status = 1
          AND p.perm_type = 1
        ORDER BY p.sort_order
        """)
    List<Permission> selectMenusByUserId(@Param("userId") Long userId);

    /** 按用户 + 登录角色查菜单（perm_type = 1） */
    @Select("""
        SELECT DISTINCT p.id, p.parent_id, p.perm_name, p.perm_code,
               p.path, p.sort_order
        FROM sys_permission p
        INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
        INNER JOIN sys_role r ON r.id = rp.role_id
        INNER JOIN sys_user_role ur ON ur.role_id = r.id
        WHERE ur.user_id = #{userId}
          AND r.role_code = #{roleCode}
          AND p.deleted = 0
          AND p.status = 1
          AND p.perm_type = 1
          AND r.deleted = 0
          AND r.status = 1
        ORDER BY p.sort_order
        """)
    List<Permission> selectMenusByUserIdAndRole(
            @Param("userId") Long userId, @Param("roleCode") String roleCode);
}
