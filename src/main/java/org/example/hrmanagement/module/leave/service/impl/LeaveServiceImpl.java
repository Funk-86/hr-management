package org.example.hrmanagement.module.leave.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.security.EmployeeDataScope;
import org.example.hrmanagement.common.security.LoginUser;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.attendance.entity.Attendance;
import org.example.hrmanagement.module.attendance.mapper.AttendanceMapper;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.leave.dto.LeaveBalanceInitDTO;
import org.example.hrmanagement.module.leave.dto.LeaveRequestCreateDTO;
import org.example.hrmanagement.module.leave.dto.LeaveTypeCreateDTO;
import org.example.hrmanagement.module.leave.dto.LeaveTypeUpdateDTO;
import org.example.hrmanagement.module.leave.entity.LeaveBalance;
import org.example.hrmanagement.module.leave.entity.LeaveRequest;
import org.example.hrmanagement.module.leave.entity.LeaveType;
import org.example.hrmanagement.module.leave.mapper.LeaveBalanceMapper;
import org.example.hrmanagement.module.leave.mapper.LeaveRequestMapper;
import org.example.hrmanagement.module.leave.mapper.LeaveTypeMapper;
import org.example.hrmanagement.module.leave.service.LeaveService;
import org.example.hrmanagement.module.leave.vo.LeaveBalanceVO;
import org.example.hrmanagement.module.leave.vo.LeaveRequestVO;
import org.example.hrmanagement.module.leave.vo.LeaveTypeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LeaveServiceImpl implements LeaveService {
    private static final int ATTENDANCE_STATUS_LEAVE = 5;

    @Autowired
    private LeaveRequestMapper leaveRequestMapper;
    @Autowired
    private LeaveTypeMapper leaveTypeMapper;
    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private LeaveBalanceMapper leaveBalanceMapper;
    @Autowired
    private AttendanceMapper attendanceMapper;
    @Autowired
    private EmployeeDataScope employeeDataScope;

    @Override
    public List<LeaveTypeVO> getLeaveType() {
        List<LeaveType> list = leaveTypeMapper.selectList(new LambdaQueryWrapper<LeaveType>().eq(LeaveType::getStatus, 1));
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream()
                .map(this::toLeaveTypeVO)
                .toList();
    }

    @Override
    public void insertLeaveType(LeaveTypeCreateDTO dto) {
        Long count = leaveTypeMapper.selectCount(new LambdaQueryWrapper<LeaveType>().eq(LeaveType::getTypeCode, dto.getTypeCode()));
        if (count > 0) {
            throw new BusinessException("类型编码已存在");
        }
        leaveTypeMapper.insert(this.toLeaveType(dto));
    }

    @Override
    public void updateLeaveType(Long id, LeaveTypeUpdateDTO dto) {
        if (id == null) {
            throw new BusinessException("id不能为空");
        }
        LeaveType existing = leaveTypeMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("请假类型不存在");
        }
        Long count = leaveTypeMapper.selectCount(
                new LambdaQueryWrapper<LeaveType>()
                        .eq(LeaveType::getTypeCode, dto.getTypeCode())
                        .ne(LeaveType::getId, id)
        );
        if (count > 0) {
            throw new BusinessException("类型编码已存在");
        }

        existing.setTypeName(dto.getTypeName());
        existing.setTypeCode(dto.getTypeCode());
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }
        if (dto.getMaxDays() != null) {
            existing.setMaxDays(dto.getMaxDays());
        }
        leaveTypeMapper.updateById(existing);
    }

    @Override
    public void deleteLeaveType(Long id) {
        if (id == null) {
            throw new BusinessException("id不能为空");
        }
        LeaveType existing = leaveTypeMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("请假类型不存在");
        }
        Long count = leaveRequestMapper.selectCount(
                new LambdaQueryWrapper<LeaveRequest>().eq(LeaveRequest::getLeaveTypeId, id)
        );
        if (count > 0) {
            throw new BusinessException("该假期类型下存在请假记录，无法删除");
        }
        existing.setStatus(0);
        leaveTypeMapper.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requestLeave(LeaveRequestCreateDTO dto) {
        Long employeeId = SecurityUtil.requireEmployeeId();
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        if (dto.getDays() == null || dto.getDays().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("请假天数不合法");
        }
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException("该员工不存在");
        }
        LeaveType leaveType = leaveTypeMapper.selectById(dto.getLeaveTypeId());
        if (leaveType == null) {
            throw new BusinessException("请假类型不存在");
        }
        if (leaveType.getStatus() == null || leaveType.getStatus() != 1) {
            throw new BusinessException("请假类型不可用");
        }

        int year = dto.getStartTime().getYear();
        if (leaveType.getMaxDays() != null) {
            LeaveBalance balance = ensureYearBalance(employeeId, leaveType, year);
            BigDecimal remaining = remainingOf(balance);
            if (dto.getDays().compareTo(remaining) > 0) {
                throw new BusinessException("请假天数超过剩余额度（剩余 " + remaining.stripTrailingZeros().toPlainString() + " 天）");
            }
            balance.setPendingDays(nz(balance.getPendingDays()).add(dto.getDays()));
            leaveBalanceMapper.updateById(balance);
        }

        Long conflictCount = leaveRequestMapper.selectCount(
                new LambdaQueryWrapper<LeaveRequest>()
                        .eq(LeaveRequest::getEmployeeId, employeeId)
                        .in(LeaveRequest::getStatus, 0, 1)
                        .lt(LeaveRequest::getStartTime, dto.getEndTime())
                        .gt(LeaveRequest::getEndTime, dto.getStartTime())
        );
        if (conflictCount > 0) {
            throw new BusinessException("该时间段已有请假申请，请调整时间");
        }
        leaveRequestMapper.insert(toLeaveRequest(dto, employeeId));
    }

    @Override
    public LeaveRequestVO getLeaveRequestById(Long id) {
        if (id == null) {
            throw new BusinessException("id不能为空");
        }
        LeaveRequest leaveRequest = leaveRequestMapper.selectById(id);
        if (leaveRequest == null) {
            throw new BusinessException("请假记录不存在");
        }
        Employee employee = employeeMapper.selectById(leaveRequest.getEmployeeId());
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        LeaveType leaveType = leaveTypeMapper.selectById(leaveRequest.getLeaveTypeId());
        if (leaveType == null) {
            throw new BusinessException("请假类型不存在");
        }
        return toLeaveRequestVO(leaveRequest, leaveType.getTypeName(), employee.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveLeave(Long id, String approveRemark) {
        LeaveRequest leaveRequest = loadPendingRequest(id);
        LoginUser user = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        leaveRequest.setApproverId(user.getUserId());
        leaveRequest.setStatus(1);
        leaveRequest.setApproveTime(LocalDateTime.now());
        leaveRequest.setApproveRemark(approveRemark);
        leaveRequestMapper.updateById(leaveRequest);

        movePendingToUsed(leaveRequest);
        upsertLeaveAttendance(leaveRequest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectLeave(Long id, String approveRemark) {
        LeaveRequest leaveRequest = loadPendingRequest(id);
        LoginUser user = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        leaveRequest.setApproverId(user.getUserId());
        leaveRequest.setStatus(2);
        leaveRequest.setApproveTime(LocalDateTime.now());
        leaveRequest.setApproveRemark(approveRemark);
        leaveRequestMapper.updateById(leaveRequest);

        releasePending(leaveRequest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelLeave(Long id, String approveRemark) {
        LeaveRequest leaveRequest = loadPendingRequest(id);
        leaveRequest.setStatus(3);
        leaveRequest.setApproveTime(LocalDateTime.now());
        if (approveRemark != null) {
            leaveRequest.setApproveRemark(approveRemark);
        }
        leaveRequestMapper.updateById(leaveRequest);
        releasePending(leaveRequest);
    }

    @Override
    public PageResult<LeaveRequestVO> getLeaveRequest(PageQuery page) {
        LambdaQueryWrapper<LeaveRequest> wrapper = new LambdaQueryWrapper<>();
        applyRequestScop(wrapper);
        wrapper.orderByDesc(LeaveRequest::getCreatedAt);

        IPage<LeaveRequest> iPage = leaveRequestMapper.selectPage(
                new Page<>(page.getPageNum(), page.getPageSize()), wrapper);

        List<LeaveRequest> leaveRequests = iPage.getRecords();
        if (leaveRequests == null || leaveRequests.isEmpty()) {
            return PageResult.empty();
        }
        Set<Long> employeeId = leaveRequests.stream().map(LeaveRequest::getEmployeeId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> leaveTypeId = leaveRequests.stream().map(LeaveRequest::getLeaveTypeId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> employeeName = employeeId.isEmpty()
                ? Map.of()
                : employeeMapper.selectBatchIds(employeeId).stream().collect(Collectors.toMap(Employee::getId, Employee::getName));
        Map<Long, String> leaveType = leaveTypeId.isEmpty()
                ? Map.of()
                : leaveTypeMapper.selectBatchIds(leaveTypeId).stream().collect(Collectors.toMap(LeaveType::getId, LeaveType::getTypeName));
        List<LeaveRequestVO> vos = leaveRequests.stream()
                .map(lr -> toLeaveRequestVO(lr, leaveType.get(lr.getLeaveTypeId()), employeeName.get(lr.getEmployeeId())))
                .collect(Collectors.toList());

        PageResult<LeaveRequestVO> result = new PageResult<>();
        result.setRecords(vos);
        result.setTotal(iPage.getTotal());
        result.setPageNum(iPage.getCurrent());
        result.setPageSize(iPage.getSize());
        result.setPages(iPage.getPages());
        return result;
    }

    @Override
    public List<LeaveBalanceVO> getMyBalances(Integer year) {
        Long employeeId = SecurityUtil.requireEmployeeId();
        int y = year != null ? year : LocalDate.now().getYear();
        return listBalancesInternal(List.of(employeeId), y);
    }

    @Override
    public List<LeaveBalanceVO> listBalances(Long employeeId, Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        List<Long> empIds;
        if (employeeId != null) {
            employeeDataScope.assertCanView(employeeId);
            empIds = List.of(employeeId);
        } else {
            empIds = resolveScopedEmployeeIds();
        }
        if (empIds.isEmpty()) {
            return List.of();
        }
        return listBalancesInternal(empIds, y);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initBalances(LeaveBalanceInitDTO dto) {
        if (!SecurityUtil.isHrStaff()) {
            throw new BusinessException("仅 HR/超管可初始化余额");
        }
        int year = dto.getYear() != null ? dto.getYear() : LocalDate.now().getYear();
        boolean overwrite = Boolean.TRUE.equals(dto.getOverwriteQuota());

        List<LeaveType> types = leaveTypeMapper.selectList(
                new LambdaQueryWrapper<LeaveType>()
                        .eq(LeaveType::getStatus, 1)
                        .isNotNull(LeaveType::getMaxDays)
        );
        List<Employee> employees = employeeMapper.selectList(
                new LambdaQueryWrapper<Employee>().in(Employee::getStatus, 1, 2)
        );
        for (Employee emp : employees) {
            for (LeaveType type : types) {
                LeaveBalance existing = findBalance(emp.getId(), type.getId(), year);
                BigDecimal quota = BigDecimal.valueOf(type.getMaxDays());
                if (existing == null) {
                    LeaveBalance bal = new LeaveBalance();
                    bal.setEmployeeId(emp.getId());
                    bal.setLeaveTypeId(type.getId());
                    bal.setYear(year);
                    bal.setQuotaDays(quota);
                    bal.setUsedDays(BigDecimal.ZERO);
                    bal.setPendingDays(BigDecimal.ZERO);
                    leaveBalanceMapper.insert(bal);
                } else if (overwrite) {
                    existing.setQuotaDays(quota);
                    leaveBalanceMapper.updateById(existing);
                }
            }
        }
    }

    private LeaveRequest loadPendingRequest(Long id) {
        if (id == null) {
            throw new BusinessException("id不能为空");
        }
        LeaveRequest leaveRequest = leaveRequestMapper.selectById(id);
        if (leaveRequest == null) {
            throw new BusinessException("请假不存在");
        }
        if (leaveRequest.getStatus() != 0) {
            throw new BusinessException("该申请已处理，无法再次处理");
        }
        return leaveRequest;
    }

    private void movePendingToUsed(LeaveRequest leaveRequest) {
        LeaveType leaveType = leaveTypeMapper.selectById(leaveRequest.getLeaveTypeId());
        if (leaveType == null || leaveType.getMaxDays() == null) {
            return;
        }
        int year = leaveRequest.getStartTime().getYear();
        LeaveBalance balance = ensureYearBalance(leaveRequest.getEmployeeId(), leaveType, year);
        BigDecimal days = nz(leaveRequest.getDays());
        BigDecimal pending = nz(balance.getPendingDays());
        if (pending.compareTo(days) < 0) {
            balance.setPendingDays(BigDecimal.ZERO);
        } else {
            balance.setPendingDays(pending.subtract(days));
        }
        balance.setUsedDays(nz(balance.getUsedDays()).add(days));
        leaveBalanceMapper.updateById(balance);
    }

    private void releasePending(LeaveRequest leaveRequest) {
        LeaveType leaveType = leaveTypeMapper.selectById(leaveRequest.getLeaveTypeId());
        if (leaveType == null || leaveType.getMaxDays() == null) {
            return;
        }
        int year = leaveRequest.getStartTime().getYear();
        LeaveBalance balance = findBalance(leaveRequest.getEmployeeId(), leaveType.getId(), year);
        if (balance == null) {
            return;
        }
        BigDecimal days = nz(leaveRequest.getDays());
        BigDecimal pending = nz(balance.getPendingDays());
        if (pending.compareTo(days) < 0) {
            balance.setPendingDays(BigDecimal.ZERO);
        } else {
            balance.setPendingDays(pending.subtract(days));
        }
        leaveBalanceMapper.updateById(balance);
    }

    private void upsertLeaveAttendance(LeaveRequest leaveRequest) {
        LocalDate start = leaveRequest.getStartTime().toLocalDate();
        LocalDate end = leaveRequest.getEndTime().toLocalDate();
        String remark = "请假单#" + leaveRequest.getId();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Attendance existing = attendanceMapper.selectOne(
                    new LambdaQueryWrapper<Attendance>()
                            .eq(Attendance::getEmployeeId, leaveRequest.getEmployeeId())
                            .eq(Attendance::getAttendDate, d)
            );
            if (existing != null) {
                existing.setStatus(ATTENDANCE_STATUS_LEAVE);
                existing.setRemark(remark);
                attendanceMapper.updateById(existing);
            } else {
                Attendance attendance = new Attendance();
                attendance.setEmployeeId(leaveRequest.getEmployeeId());
                attendance.setAttendDate(d);
                attendance.setStatus(ATTENDANCE_STATUS_LEAVE);
                attendance.setRemark(remark);
                attendanceMapper.insert(attendance);
            }
        }
    }

    private LeaveBalance ensureYearBalance(Long employeeId, LeaveType leaveType, int year) {
        LeaveBalance existing = findBalance(employeeId, leaveType.getId(), year);
        if (existing != null) {
            return existing;
        }
        LeaveBalance bal = new LeaveBalance();
        bal.setEmployeeId(employeeId);
        bal.setLeaveTypeId(leaveType.getId());
        bal.setYear(year);
        bal.setQuotaDays(BigDecimal.valueOf(leaveType.getMaxDays()));
        bal.setUsedDays(BigDecimal.ZERO);
        bal.setPendingDays(BigDecimal.ZERO);
        leaveBalanceMapper.insert(bal);
        return bal;
    }

    private LeaveBalance findBalance(Long employeeId, Long leaveTypeId, int year) {
        return leaveBalanceMapper.selectOne(
                new LambdaQueryWrapper<LeaveBalance>()
                        .eq(LeaveBalance::getEmployeeId, employeeId)
                        .eq(LeaveBalance::getLeaveTypeId, leaveTypeId)
                        .eq(LeaveBalance::getYear, year)
        );
    }

    private List<LeaveBalanceVO> listBalancesInternal(List<Long> employeeIds, int year) {
        List<LeaveBalance> balances = leaveBalanceMapper.selectList(
                new LambdaQueryWrapper<LeaveBalance>()
                        .in(LeaveBalance::getEmployeeId, employeeIds)
                        .eq(LeaveBalance::getYear, year)
                        .orderByAsc(LeaveBalance::getEmployeeId)
                        .orderByAsc(LeaveBalance::getLeaveTypeId)
        );
        if (balances.isEmpty()) {
            return List.of();
        }
        Set<Long> empIds = balances.stream().map(LeaveBalance::getEmployeeId).collect(Collectors.toSet());
        Set<Long> typeIds = balances.stream().map(LeaveBalance::getLeaveTypeId).collect(Collectors.toSet());
        Map<Long, Employee> empMap = employeeMapper.selectBatchIds(empIds).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));
        Map<Long, LeaveType> typeMap = leaveTypeMapper.selectBatchIds(typeIds).stream()
                .collect(Collectors.toMap(LeaveType::getId, t -> t));

        List<LeaveBalanceVO> result = new ArrayList<>();
        for (LeaveBalance bal : balances) {
            LeaveBalanceVO vo = new LeaveBalanceVO();
            vo.setId(bal.getId());
            vo.setEmployeeId(bal.getEmployeeId());
            Employee emp = empMap.get(bal.getEmployeeId());
            vo.setEmployeeName(emp != null ? emp.getName() : null);
            vo.setLeaveTypeId(bal.getLeaveTypeId());
            LeaveType type = typeMap.get(bal.getLeaveTypeId());
            if (type != null) {
                vo.setLeaveTypeName(type.getTypeName());
                vo.setLeaveTypeCode(type.getTypeCode());
            }
            vo.setYear(bal.getYear());
            vo.setQuotaDays(bal.getQuotaDays());
            vo.setUsedDays(nz(bal.getUsedDays()));
            vo.setPendingDays(nz(bal.getPendingDays()));
            vo.setRemainingDays(remainingOf(bal));
            result.add(vo);
        }
        return result;
    }

    private List<Long> resolveScopedEmployeeIds() {
        if (SecurityUtil.isHrStaff()) {
            return employeeMapper.selectList(new LambdaQueryWrapper<>()).stream()
                    .map(Employee::getId).toList();
        }
        if (SecurityUtil.hasRole("EMPLOYEE") && !SecurityUtil.isManagerUp()) {
            return List.of(SecurityUtil.requireEmployeeId());
        }
        Long deptId = SecurityUtil.requireDeptId();
        return employeeMapper.selectList(
                new LambdaQueryWrapper<Employee>().eq(Employee::getDeptId, deptId)
        ).stream().map(Employee::getId).toList();
    }

    private static BigDecimal remainingOf(LeaveBalance balance) {
        return nz(balance.getQuotaDays())
                .subtract(nz(balance.getUsedDays()))
                .subtract(nz(balance.getPendingDays()));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private void applyRequestScop(LambdaQueryWrapper<LeaveRequest> wrapper) {
        if (SecurityUtil.hasRole("EMPLOYEE") && !SecurityUtil.isManagerUp()) {
            wrapper.eq(LeaveRequest::getEmployeeId, SecurityUtil.requireEmployeeId());
            return;
        }
        if (SecurityUtil.isHrStaff()) {
            return;
        }
        Long deptId = SecurityUtil.requireDeptId();
        List<Long> empIds = employeeMapper.selectList(
                new LambdaQueryWrapper<Employee>().eq(Employee::getDeptId, deptId)
        ).stream().map(Employee::getId).toList();
        if (empIds.isEmpty()) {
            wrapper.eq(LeaveRequest::getEmployeeId, -1L);
            return;
        }
        wrapper.in(LeaveRequest::getEmployeeId, empIds);
    }

    private LeaveRequestVO toLeaveRequestVO(LeaveRequest leaveRequest, String leaveType, String employeeName) {
        LeaveRequestVO leaveRequestVO = new LeaveRequestVO();
        leaveRequestVO.setLeaveType(leaveType);
        leaveRequestVO.setEmployeeName(employeeName);
        leaveRequestVO.setStartTime(leaveRequest.getStartTime());
        leaveRequestVO.setEndTime(leaveRequest.getEndTime());
        leaveRequestVO.setDays(leaveRequest.getDays());
        leaveRequestVO.setStatus(leaveRequest.getStatus());
        leaveRequestVO.setReason(leaveRequest.getReason());
        leaveRequestVO.setId(leaveRequest.getId());
        return leaveRequestVO;
    }

    private LeaveRequest toLeaveRequest(LeaveRequestCreateDTO dto, Long employeeId) {
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployeeId(employeeId);
        leaveRequest.setLeaveTypeId(dto.getLeaveTypeId());
        leaveRequest.setStartTime(dto.getStartTime());
        leaveRequest.setEndTime(dto.getEndTime());
        leaveRequest.setDays(dto.getDays());
        leaveRequest.setReason(dto.getReason());
        leaveRequest.setStatus(0);
        return leaveRequest;
    }

    private LeaveType toLeaveType(LeaveTypeCreateDTO dto) {
        LeaveType leaveType = new LeaveType();
        leaveType.setTypeCode(dto.getTypeCode());
        leaveType.setTypeName(dto.getTypeName());
        leaveType.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        leaveType.setMaxDays(dto.getMaxDays());
        return leaveType;
    }

    private LeaveTypeVO toLeaveTypeVO(LeaveType leaveType) {
        LeaveTypeVO vo = new LeaveTypeVO();
        vo.setId(leaveType.getId());
        vo.setTypeCode(leaveType.getTypeCode());
        vo.setTypeName(leaveType.getTypeName());
        vo.setStatus(leaveType.getStatus());
        vo.setMaxDays(leaveType.getMaxDays());
        return vo;
    }
}
