package org.example.hrmanagement.common.util;

import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.result.ResultCode;
import org.example.hrmanagement.common.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SecurityUtil {

    private SecurityUtil() {
    }

    /** 获取当前登录用户，未登录则抛 401 */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }

    /** 尝试获取当前用户，未登录返回 null */
    public static LoginUser getLoginUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    /** 是否拥有指定角色（roleCode 与 sys_role.role_code 一致，如 HR_ADMIN） */
    public static boolean hasRole(String roleCode) {
        List<String> roles = getRoles();
        return roles.contains(roleCode);
    }

    /** 是否拥有任一角色 */
    public static boolean hasAnyRole(String... roleCodes) {
        List<String> roles = getRoles();
        return Arrays.stream(roleCodes).anyMatch(roles::contains);
    }

    /** 当前用户角色列表 */
    public static List<String> getRoles() {
        LoginUser user = getLoginUser();
        return user.getRoles() != null ? user.getRoles() : Collections.emptyList();
    }

    /** 当前用户权限码列表 */
    public static List<String> getPermissions() {
        LoginUser user = getLoginUser();
        return user.getPermissions() != null ? user.getPermissions() : Collections.emptyList();
    }

    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    /** 关联员工 ID，未关联时返回 null */
    public static Long getEmployeeId() {
        return getLoginUser().getEmployeeId();
    }

    /** 关联员工 ID，未关联时抛业务异常（打卡、请假等自助接口用） */
    public static Long requireEmployeeId() {
        Long employeeId = getEmployeeId();
        if (employeeId == null) {
            throw new BusinessException("当前账号未关联员工，无法操作");
        }
        return employeeId;
    }

    /** 关联部门 ID，未关联时返回 null */
    public static Long getDeptId() {
        return getLoginUser().getDeptId();
    }

    /** 关联部门 ID，未关联时抛业务异常（部门经理按部门过滤数据时用） */
    public static Long requireDeptId() {
        Long deptId = getDeptId();
        if (deptId == null) {
            throw new BusinessException("当前账号未关联部门，无法操作");
        }
        return deptId;
    }

    /** HR 管理员或超级管理员 */
    public static boolean isHrStaff() {
        return hasAnyRole("SUPER_ADMIN", "HR_ADMIN");
    }

    /** 部门经理及以上 */
    public static boolean isManagerUp() {
        return hasAnyRole("SUPER_ADMIN", "HR_ADMIN", "DEPT_MANAGER");
    }

    public static boolean isEmployeeManager() {
        return hasAnyRole("HR_ADMIN", "DEPT_MANAGER","HR_MANAGER");
    }

    public static boolean isSuperAdmin() {
        return hasAnyRole( "SUPER_ADMIN");
    }
}
