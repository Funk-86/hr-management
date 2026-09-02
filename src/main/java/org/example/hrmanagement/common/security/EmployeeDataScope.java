package org.example.hrmanagement.common.security;

import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 员工数据范围校验：与列表查询 {@code buildListWrapper} 规则一致。
 */
@Component
@RequiredArgsConstructor
public class EmployeeDataScope {

    private final EmployeeMapper employeeMapper;

    /** 查看员工详情/关联数据 */
    public void assertCanView(Long employeeId) {
        if (employeeId == null) {
            throw new BusinessException("员工ID不能为空");
        }
        if (SecurityUtil.isHrStaff()) {
            return;
        }
        if (SecurityUtil.hasRole("EMPLOYEE") && !SecurityUtil.isManagerUp()) {
            if (!Objects.equals(employeeId, SecurityUtil.requireEmployeeId())) {
                throw new BusinessException("无权查看该员工信息");
            }
            return;
        }
        Long deptId = SecurityUtil.requireDeptId();
        Employee emp = employeeMapper.selectById(employeeId);
        if (emp == null || !Objects.equals(emp.getDeptId(), deptId)) {
            throw new BusinessException("无权查看该员工信息");
        }
    }

    /** 修改/删除员工（经理及以上或 HR） */
    public void assertCanManage(Long employeeId) {
        assertCanView(employeeId);
        if (SecurityUtil.isHrStaff()) {
            return;
        }
        if (SecurityUtil.hasRole("EMPLOYEE") && !SecurityUtil.isManagerUp()) {
            throw new BusinessException("无权操作员工信息");
        }
    }
}
