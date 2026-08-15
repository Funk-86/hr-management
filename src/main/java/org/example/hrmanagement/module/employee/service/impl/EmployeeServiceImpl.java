package org.example.hrmanagement.module.employee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.file.AvatarService;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.auth.entity.Role;
import org.example.hrmanagement.module.auth.entity.User;
import org.example.hrmanagement.module.auth.entity.UserRole;
import org.example.hrmanagement.module.auth.mapper.RoleMapper;
import org.example.hrmanagement.module.auth.mapper.UserMapper;
import org.example.hrmanagement.module.auth.mapper.UserRoleMapper;
import org.example.hrmanagement.module.department.entity.Department;
import org.example.hrmanagement.module.department.mapper.DepartmentMapper;
import org.example.hrmanagement.module.employee.dto.EmployeeCreateDTO;
import org.example.hrmanagement.module.employee.dto.EmployeeUpdateDTO;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.employee.service.EmployeeService;
import org.example.hrmanagement.module.employee.vo.EmployeeVO;
import org.example.hrmanagement.module.position.entity.Position;
import org.example.hrmanagement.module.position.mapper.PositionMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final PositionMapper positionMapper;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final AvatarService avatarService;


    private static final int EXPORT_LIMIT = 5000;

    @Override
    public PageResult<EmployeeVO> list(PageQuery page){
        LambdaQueryWrapper<Employee> wrapper = buildListWrapper();

        IPage<Employee> iPage = employeeMapper.selectPage(
                new Page<>(page.getPageNum(), page.getPageSize()), wrapper);

        List<Employee> list = iPage.getRecords();
        if (list == null || list.isEmpty()) {
            return PageResult.empty();
        }

        List<EmployeeVO> vos = toVoList(list);

        PageResult<EmployeeVO> result = new PageResult<>();
        result.setRecords(vos);
        result.setTotal(iPage.getTotal());
        result.setPageNum(iPage.getCurrent());
        result.setPageSize(iPage.getSize());
        result.setPages(iPage.getPages());
        return result;
    }

    @Override
    public List<EmployeeVO> listForExport() {
        LambdaQueryWrapper<Employee> wrapper = buildListWrapper();
        wrapper.last("LIMIT " + (EXPORT_LIMIT + 1));
        List<Employee> list = employeeMapper.selectList(wrapper);
        if (list.size() > EXPORT_LIMIT) {
            throw new BusinessException("导出数据超过 " + EXPORT_LIMIT + " 条，请缩小范围后重试");
        }
        return toVoList(list);
    }

    private LambdaQueryWrapper<Employee> buildListWrapper() {
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        if (SecurityUtil.hasRole("DEPT_MANAGER") && !SecurityUtil.isHrStaff()) {
            Long deptId = SecurityUtil.requireDeptId();
            wrapper.eq(Employee::getDeptId, deptId);
        } else if (SecurityUtil.hasRole("EMPLOYEE") && !SecurityUtil.isManagerUp()) {
            Long employeeId = SecurityUtil.requireEmployeeId();
            wrapper.eq(Employee::getId, employeeId);
        }
        wrapper.orderByDesc(Employee::getCreatedAt);
        return wrapper;
    }

    private List<EmployeeVO> toVoList(List<Employee> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        Set<Long> deptIds = list.stream()
                .map(Employee::getDeptId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> positionIds = list.stream()
                .map(Employee::getPositionId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, String> deptNameMap = deptIds.isEmpty()
                ? Map.of()
                : departmentMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(Department::getId, Department::getDeptName));
        Map<Long, String> positionNameMap = positionIds.isEmpty()
                ? Map.of()
                : positionMapper.selectBatchIds(positionIds).stream()
                        .collect(Collectors.toMap(Position::getId, Position::getPositionName));

        return list.stream()
                .map(employee -> toVO(employee,
                        deptNameMap.get(employee.getDeptId()),
                        positionNameMap.get(employee.getPositionId())))
                .collect(Collectors.toList());
    }

    @Override
    public EmployeeVO findById(Long id) {
        if (id == null) {
            throw new BusinessException("id为空");
        }

        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BusinessException("该员工不存在");
        }

        if (employee.getDeptId() == null) {
            throw new BusinessException("员工未关联部门");
        }
        Department department = departmentMapper.selectById(employee.getDeptId());
        if (department == null) {
            throw new BusinessException("员工关联的部门不存在");
        }

        if (employee.getPositionId() == null) {
            throw new BusinessException("员工未关联岗位");
        }
        Position position = positionMapper.selectById(employee.getPositionId());
        if (position == null) {
            throw new BusinessException("员工关联的岗位不存在");
        }

        return toVO(employee, department.getDeptName(), position.getPositionName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insert(EmployeeCreateDTO dto) {
        Department department = departmentMapper.selectById(dto.getDeptId());
        if (department == null) {
            throw new BusinessException("部门不存在");
        }
        Position position = positionMapper.selectById(dto.getPositionId());
        if (position == null) {
            throw new BusinessException("岗位不存在");
        }
        if (!position.getDeptId().equals(dto.getDeptId())) {
            throw new BusinessException("岗位不属于所选部门");
        }
        Long count = employeeMapper.selectCount(
                new LambdaQueryWrapper<Employee>().eq(Employee::getEmpNo, dto.getEmpNo())
        );
        if (count > 0) {
            throw new BusinessException("工号已存在");
        }
        String username = dto.getUsername().trim();
        Long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (userCount > 0) {
            throw new BusinessException("该用户名已存在");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BusinessException("密码不能为空");
        }

        String roleCode = dto.getRoleCode() != null && !dto.getRoleCode().isBlank()
                ? dto.getRoleCode()
                : "EMPLOYEE";
        if ("SUPER_ADMIN".equals(roleCode)) {
            throw new BusinessException("不允许通过员工开户创建超级管理员");
        }
        Role role = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>()
                        .eq(Role::getRoleCode, roleCode)
                        .eq(Role::getStatus, 1)
        );
        if (role == null) {
            throw new BusinessException("角色不存在或已禁用");
        }

        Employee employee = toEntity(dto);
        employeeMapper.insert(employee);

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmployeeId(employee.getId());
        user.setStatus(1);
        userMapper.insert(user);

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
        log.info("员工入职成功: empNo={}, name={}, role={}", dto.getEmpNo(), dto.getName(), roleCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, EmployeeUpdateDTO dto) {
        if (id == null) {
            throw new BusinessException("id不能为空");
        }
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        Long deptId = dto.getDeptId() != null ? dto.getDeptId() : employee.getDeptId();
        Long positionId = dto.getPositionId() != null ? dto.getPositionId() : employee.getPositionId();
        if (dto.getDeptId() != null) {
            Department department = departmentMapper.selectById(dto.getDeptId());
            if (department == null) {
                throw new BusinessException("部门不存在");
            }
        }
        if (dto.getPositionId() != null) {
            Position position = positionMapper.selectById(dto.getPositionId());
            if (position == null) {
                throw new BusinessException("岗位不存在");
            }
        }
        if (dto.getDeptId() != null || dto.getPositionId() != null) {
            Position position = positionMapper.selectById(positionId);
            if (position == null) {
                throw new BusinessException("岗位不存在");
            }
            if (!position.getDeptId().equals(deptId)) {
                throw new BusinessException("岗位不属于所选部门");
            }
        }

        if (dto.getName() != null) employee.setName(dto.getName());
        if (dto.getDeptId() != null) employee.setDeptId(dto.getDeptId());
        if (dto.getPositionId() != null) employee.setPositionId(dto.getPositionId());
        if (dto.getGender() != null) employee.setGender(dto.getGender());
        if (dto.getEmploymentType() != null) employee.setEmploymentType(dto.getEmploymentType());
        if (dto.getStatus() != null) {
            employee.setStatus(dto.getStatus());
            if (dto.getStatus() == 3) {
                userMapper.update(null, new LambdaUpdateWrapper<User>()
                        .eq(User::getEmployeeId, id)
                        .set(User::getStatus, 0));
            } else {
                userMapper.update(null, new LambdaUpdateWrapper<User>()
                        .eq(User::getEmployeeId, id)
                        .set(User::getStatus, 1));
            }
        }
        if (dto.getPhone() != null) employee.setPhone(dto.getPhone());
        if (dto.getEmail() != null) employee.setEmail(dto.getEmail());
        if (dto.getIdCard() != null) employee.setIdCard(dto.getIdCard());
        if (dto.getAvatar() != null) employee.setAvatar(dto.getAvatar());
        if (dto.getRemark() != null) employee.setRemark(dto.getRemark());
        employeeMapper.updateById(employee);

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            if (!SecurityUtil.isSuperAdmin()) {
                throw new BusinessException("仅超级管理员可修改员工登录密码");
            }
            User user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getEmployeeId, id));
            if (user == null) {
                throw new BusinessException("该员工未关联登录账号，无法修改密码");
            }
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, user.getId())
                    .set(User::getPassword, passwordEncoder.encode(dto.getPassword().trim())));
        }
    }

    @Override
    public void delete(Long id) {
        if (id == null) {
            throw new BusinessException("id不能为空");
        }
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        Long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmployeeId, id)
        );
        if (userCount > 0) {
            throw new BusinessException("该员工已关联系统用户，无法删除");
        }
        employeeMapper.deleteById(id);
    }

    private Employee toEntity(EmployeeCreateDTO dto) {
        Employee employee = new Employee();
        employee.setDeptId(dto.getDeptId());
        employee.setPositionId(dto.getPositionId());
        employee.setName(dto.getName());
        employee.setAvatar(dto.getAvatar());
        employee.setGender(dto.getGender() !=null ? dto.getGender() : 1);
        employee.setEmail(dto.getEmail());
        employee.setPhone(dto.getPhone());
        employee.setEmpNo(dto.getEmpNo());
        employee.setEmploymentType(dto.getEmploymentType() !=null ? dto.getEmploymentType() : 1);
        employee.setHireDate(dto.getHireDate());
        employee.setIdCard(dto.getIdCard());
        employee.setProbationEnd(dto.getProbationEnd());
        employee.setRemark(dto.getRemark());
        employee.setStatus(dto.getStatus() !=null ? dto.getStatus() : 1);
        return employee;
    }

    private EmployeeVO toVO(Employee employee,String deptName,String positionName) {
       EmployeeVO vo=new EmployeeVO();
       vo.setId(employee.getId());
       vo.setName(employee.getName());
       vo.setAvatar(avatarService.resolveEmployeeAvatarUrl(employee));
       vo.setEmail(employee.getEmail());
       vo.setPhone(employee.getPhone());
       vo.setGender(employee.getGender());
       vo.setDeptId(employee.getDeptId());
       vo.setPositionId(employee.getPositionId());
       vo.setHireDate(employee.getHireDate());
       vo.setIdCard(employee.getIdCard());
       vo.setEmploymentType(employee.getEmploymentType());
       vo.setEmpNo(employee.getEmpNo());
       vo.setStatus(employee.getStatus());
       vo.setRemark(employee.getRemark());
       vo.setDeptName(deptName);
       vo.setPositionName(positionName);
       vo.setProbationEnd(employee.getProbationEnd());
       return vo;
    }
}
