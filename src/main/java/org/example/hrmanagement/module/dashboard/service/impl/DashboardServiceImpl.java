package org.example.hrmanagement.module.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.attendance.entity.Attendance;
import org.example.hrmanagement.module.attendance.mapper.AttendanceMapper;
import org.example.hrmanagement.module.dashboard.dto.DashboardDTO;
import org.example.hrmanagement.module.dashboard.service.DashboardService;
import org.example.hrmanagement.module.dashboard.vo.DashboardVO;
import org.example.hrmanagement.module.dashboard.vo.HrStatsVO;
import org.example.hrmanagement.module.department.entity.Department;
import org.example.hrmanagement.module.department.mapper.DepartmentMapper;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.leave.entity.LeaveRequest;
import org.example.hrmanagement.module.leave.mapper.LeaveRequestMapper;
import org.example.hrmanagement.module.project.entity.Project;
import org.example.hrmanagement.module.project.mapper.ProjectMapper;
import org.example.hrmanagement.module.project.service.ProjectService;
import org.example.hrmanagement.module.salary.entity.Salary;
import org.example.hrmanagement.module.salary.mapper.SalaryMapper;
import org.example.hrmanagement.module.task.entity.Task;
import org.example.hrmanagement.module.task.mapper.TaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AttendanceMapper attendanceMapper;
    private final EmployeeMapper employeeMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final DepartmentMapper departmentMapper;
    private final ProjectService projectService;
    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;
    private final SalaryMapper salaryMapper;

    @Override
    public DashboardVO getDashboard(DashboardDTO dto) {
        String yearMonthText = StringUtils.hasText(dto.getYearMonth())
                ? dto.getYearMonth()
                : YearMonth.now().format(YEAR_MONTH);
        YearMonth ym;
        try {
            ym = YearMonth.parse(yearMonthText, YEAR_MONTH);
        } catch (Exception e) {
            throw new BusinessException("yearMonth 格式应为 yyyy-MM，如 2026-06");
        }

        int year = ym.getYear();
        int month = ym.getMonthValue();
        int totalDays = ym.lengthOfMonth();

        // 汇总指标
        long totalEmployees = employeeMapper.selectCount(
                new LambdaQueryWrapper<Employee>().in(Employee::getStatus, 1, 2));

        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();
        long newHiresThisMonth = employeeMapper.selectCount(
                new LambdaQueryWrapper<Employee>()
                        .ge(Employee::getHireDate, monthStart)
                        .le(Employee::getHireDate, monthEnd));

        long pendingApprovals = leaveRequestMapper.selectCount(
                new LambdaQueryWrapper<LeaveRequest>().eq(LeaveRequest::getStatus, 0));

        // 部门人数分布
        List<Department> depts = departmentMapper.selectList(null);
        Map<Long, String> deptNameMap = depts.stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName));
        Map<String, Long> deptDistribution = new LinkedHashMap<>();
        for (Department dept : depts) {
            if (dept.getParentId() != null && dept.getParentId() != 0) {
                continue; // 只统计一级部门
            }
            long count = employeeMapper.selectCount(
                    new LambdaQueryWrapper<Employee>()
                            .eq(Employee::getDeptId, dept.getId())
                            .in(Employee::getStatus, 1, 2));
            deptDistribution.put(dept.getDeptName(), count);
        }

        DashboardVO vo = new DashboardVO();
        vo.setYear(year);
        vo.setMonth(month);
        vo.setTotalDays(totalDays);
        vo.setTotalEmployees(totalEmployees);
        vo.setNewHiresThisMonth(newHiresThisMonth);
        vo.setPendingApprovals(pendingApprovals);
        vo.setDepartmentDistribution(deptDistribution);
        vo.setProjectCount(projectService.countMyActiveProjects());

        // 个人考勤日历
        if (SecurityUtil.hasRole("SUPER_ADMIN") && !SecurityUtil.hasAnyRole("HR_ADMIN", "DEPT_MANAGER", "EMPLOYEE")) {
            vo.setTeamCount(0L);
            vo.setDayStatus(new ArrayList<>(Collections.nCopies(totalDays, 0)));
            vo.setPunchDays(0);
            vo.setNormalCount(0);
            vo.setLateCount(0);
            return vo;
        }

        Long employeeId = SecurityUtil.requireEmployeeId();
        Employee me = employeeMapper.selectById(employeeId);
        long teamCount = 0L;
        if (me != null && me.getDeptId() != null) {
            teamCount = employeeMapper.selectCount(
                    new LambdaQueryWrapper<Employee>()
                            .eq(Employee::getDeptId, me.getDeptId())
                            .in(Employee::getStatus, 1, 2));
        }
        vo.setTeamCount(teamCount);

        LocalDate start = ym.atDay(1);
        LocalDate endExclusive = ym.plusMonths(1).atDay(1);

        List<Attendance> records = attendanceMapper.selectList(
                new LambdaQueryWrapper<Attendance>()
                        .eq(Attendance::getEmployeeId, employeeId)
                        .ge(Attendance::getAttendDate, start)
                        .lt(Attendance::getAttendDate, endExclusive)
                        .orderByAsc(Attendance::getAttendDate));

        List<Integer> dayStatus = new ArrayList<>(Collections.nCopies(totalDays, 0));
        for (Attendance record : records) {
            int dayIndex = record.getAttendDate().getDayOfMonth() - 1;
            dayStatus.set(dayIndex, record.getStatus());
        }

        int punchDays = 0, normalCount = 0, lateCount = 0;
        for (Integer status : dayStatus) {
            if (status != null && status > 0) punchDays++;
            if (Integer.valueOf(1).equals(status)) normalCount++;
            if (Integer.valueOf(2).equals(status)) lateCount++;
        }

        vo.setDayStatus(dayStatus);
        vo.setPunchDays(punchDays);
        vo.setNormalCount(normalCount);
        vo.setLateCount(lateCount);
        return vo;
    }

    @Override
    public HrStatsVO getHrStats(String yearMonth) {
        if (!SecurityUtil.isHrStaff()) {
            throw new BusinessException("仅人事管理员可查看统计看板");
        }
        String ymText = StringUtils.hasText(yearMonth)
                ? yearMonth
                : YearMonth.now().format(YEAR_MONTH);
        YearMonth ym;
        try {
            ym = YearMonth.parse(ymText, YEAR_MONTH);
        } catch (Exception e) {
            throw new BusinessException("yearMonth 格式应为 yyyy-MM");
        }

        HrStatsVO vo = new HrStatsVO();
        vo.setYearMonth(ym.format(YEAR_MONTH));

        Long totalEmployees = employeeMapper.selectCount(
                new LambdaQueryWrapper<Employee>().in(Employee::getStatus, 1, 2));
        vo.setTotalEmployees(totalEmployees == null ? 0L : totalEmployees);

        List<Department> depts = departmentMapper.selectList(null);
        Map<String, Long> deptDistribution = new LinkedHashMap<>();
        for (Department dept : depts) {
            if (dept.getParentId() != null && dept.getParentId() != 0) {
                continue;
            }
            long count = employeeMapper.selectCount(
                    new LambdaQueryWrapper<Employee>()
                            .eq(Employee::getDeptId, dept.getId())
                            .in(Employee::getStatus, 1, 2));
            deptDistribution.put(dept.getDeptName(), count);
        }
        vo.setDepartmentDistribution(deptDistribution);

        LocalDate start = ym.atDay(1);
        LocalDate endExclusive = ym.plusMonths(1).atDay(1);
        List<Attendance> attendances = attendanceMapper.selectList(
                new LambdaQueryWrapper<Attendance>()
                        .ge(Attendance::getAttendDate, start)
                        .lt(Attendance::getAttendDate, endExclusive)
                        .in(Attendance::getStatus, 2, 3, 4));
        long late = attendances.stream().filter(a -> Objects.equals(a.getStatus(), 2)).count();
        long early = attendances.stream().filter(a -> Objects.equals(a.getStatus(), 3)).count();
        long absent = attendances.stream().filter(a -> Objects.equals(a.getStatus(), 4)).count();
        Map<String, Long> abnormal = new LinkedHashMap<>();
        abnormal.put("迟到", late);
        abnormal.put("早退", early);
        abnormal.put("缺勤", absent);
        vo.setAttendanceAbnormal(abnormal);
        vo.setAttendanceAbnormalCount(late + early + absent);

        List<Salary> salaries = salaryMapper.selectList(
                new LambdaQueryWrapper<Salary>().eq(Salary::getSalaryMonth, vo.getYearMonth()));
        BigDecimal totalAmount = salaries.stream()
                .map(Salary::getActualSalary)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long paid = salaries.stream().filter(s -> Objects.equals(s.getStatus(), 1)).count();
        long pending = salaries.stream().filter(s -> Objects.equals(s.getStatus(), 0)).count();
        vo.setSalaryTotalAmount(totalAmount);
        vo.setSalaryPaidCount(paid);
        vo.setSalaryPendingCount(pending);

        Long taskTotal = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getParentId, 0)
                        .ne(Task::getStatus, 3));
        Long taskDone = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getParentId, 0)
                        .eq(Task::getStatus, 2));
        long tt = taskTotal == null ? 0L : taskTotal;
        long td = taskDone == null ? 0L : taskDone;
        vo.setTaskTotalCount(tt);
        vo.setTaskDoneCount(td);
        vo.setTaskCompletionRate(tt == 0 ? 0 : (int) Math.round(td * 100.0 / tt));

        Long projectTotal = projectMapper.selectCount(
                new LambdaQueryWrapper<Project>().ne(Project::getStatus, 3));
        Long projectDone = projectMapper.selectCount(
                new LambdaQueryWrapper<Project>().eq(Project::getStatus, 2));
        long pt = projectTotal == null ? 0L : projectTotal;
        long pd = projectDone == null ? 0L : projectDone;
        vo.setProjectTotalCount(pt);
        vo.setProjectDoneCount(pd);
        vo.setProjectCompletionRate(pt == 0 ? 0 : (int) Math.round(pd * 100.0 / pt));

        return vo;
    }
}
