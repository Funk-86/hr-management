package org.example.hrmanagement.module.performance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.department.entity.Department;
import org.example.hrmanagement.module.department.mapper.DepartmentMapper;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.performance.dto.PerformanceQueryDTO;
import org.example.hrmanagement.module.performance.dto.PerformanceSaveDTO;
import org.example.hrmanagement.module.performance.entity.PerformanceReview;
import org.example.hrmanagement.module.performance.mapper.PerformanceReviewMapper;
import org.example.hrmanagement.module.performance.service.PerformanceService;
import org.example.hrmanagement.module.performance.vo.PerformanceReviewVO;
import org.example.hrmanagement.module.performance.vo.PerformanceTaskHintVO;
import org.example.hrmanagement.module.task.entity.Task;
import org.example.hrmanagement.module.task.entity.TaskAssignee;
import org.example.hrmanagement.module.task.mapper.TaskAssigneeMapper;
import org.example.hrmanagement.module.task.mapper.TaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerformanceServiceImpl implements PerformanceService {

    private static final Map<Integer, String> GRADE_LABEL = Map.of(
            1, "优", 2, "良", 3, "中", 4, "合格", 5, "差");
    private static final Map<Integer, String> STATUS_LABEL = Map.of(
            0, "草稿", 1, "已提交", 2, "已确认");
    private static final Pattern MONTH_KEY = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
    private static final Pattern QUARTER_KEY = Pattern.compile("^\\d{4}-Q[1-4]$");

    private final PerformanceReviewMapper reviewMapper;
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;
    private final TaskMapper taskMapper;

    @Override
    public PageResult<PerformanceReviewVO> page(PerformanceQueryDTO query) {
        Set<Long> scoped = resolveScopedEmployeeIds(query.getDeptId(), query.getEmployeeId());
        if (scoped != null && scoped.isEmpty()) {
            return PageResult.empty();
        }

        LambdaQueryWrapper<PerformanceReview> wrapper = new LambdaQueryWrapper<PerformanceReview>()
                .in(scoped != null, PerformanceReview::getEmployeeId, scoped)
                .eq(query.getEmployeeId() != null, PerformanceReview::getEmployeeId, query.getEmployeeId())
                .eq(query.getPeriodType() != null, PerformanceReview::getPeriodType, query.getPeriodType())
                .eq(StringUtils.hasText(query.getPeriodKey()), PerformanceReview::getPeriodKey, query.getPeriodKey())
                .eq(query.getStatus() != null, PerformanceReview::getStatus, query.getStatus())
                .orderByDesc(PerformanceReview::getPeriodKey)
                .orderByDesc(PerformanceReview::getId);

        IPage<PerformanceReview> iPage = reviewMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        if (iPage.getRecords() == null || iPage.getRecords().isEmpty()) {
            return PageResult.empty();
        }
        List<PerformanceReviewVO> vos = toVos(iPage.getRecords());
        PageResult<PerformanceReviewVO> result = new PageResult<>();
        result.setRecords(vos);
        result.setTotal(iPage.getTotal());
        result.setPageNum(iPage.getCurrent());
        result.setPageSize(iPage.getSize());
        result.setPages(iPage.getPages());
        return result;
    }

    @Override
    public PerformanceReviewVO getDetail(Long id) {
        PerformanceReview review = requireReview(id);
        assertCanView(review.getEmployeeId());
        return toVos(List.of(review)).get(0);
    }

    @Override
    public List<PerformanceReviewVO> listByEmployee(Long employeeId, Integer limit) {
        if (employeeId == null) {
            throw new BusinessException("员工ID不能为空");
        }
        assertCanView(employeeId);
        int size = limit == null || limit <= 0 ? 12 : Math.min(limit, 50);
        List<PerformanceReview> list = reviewMapper.selectList(
                new LambdaQueryWrapper<PerformanceReview>()
                        .eq(PerformanceReview::getEmployeeId, employeeId)
                        .ne(PerformanceReview::getStatus, 0)
                        .orderByDesc(PerformanceReview::getPeriodKey)
                        .orderByDesc(PerformanceReview::getId)
                        .last("LIMIT " + size));
        return toVos(list);
    }

    @Override
    public PerformanceTaskHintVO taskHint(Long employeeId, Integer periodType, String periodKey) {
        if (employeeId == null) {
            throw new BusinessException("请选择员工");
        }
        assertCanManageEmployee(employeeId);
        validatePeriod(periodType, periodKey);
        LocalDateTime[] range = resolvePeriodRange(periodType, periodKey.trim());
        return buildTaskHint(employeeId, periodType, periodKey.trim(), range[0], range[1]);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PerformanceSaveDTO dto) {
        if (!SecurityUtil.isManagerUp()) {
            throw new BusinessException("仅经理及以上可发起考核");
        }
        validatePeriod(dto.getPeriodType(), dto.getPeriodKey());
        String periodKey = dto.getPeriodKey().trim();
        assertCanManageEmployee(dto.getEmployeeId());

        Long exists = reviewMapper.selectCount(
                new LambdaQueryWrapper<PerformanceReview>()
                        .eq(PerformanceReview::getEmployeeId, dto.getEmployeeId())
                        .eq(PerformanceReview::getPeriodType, dto.getPeriodType())
                        .eq(PerformanceReview::getPeriodKey, periodKey));
        if (exists != null && exists > 0) {
            throw new BusinessException("该员工在此周期已有考核单");
        }

        Long reviewerId = SecurityUtil.requireEmployeeId();
        LocalDateTime[] range = resolvePeriodRange(dto.getPeriodType(), periodKey);
        PerformanceTaskHintVO hint = buildTaskHint(dto.getEmployeeId(), dto.getPeriodType(), periodKey, range[0], range[1]);

        PerformanceReview review = new PerformanceReview();
        review.setEmployeeId(dto.getEmployeeId());
        review.setPeriodType(dto.getPeriodType());
        review.setPeriodKey(periodKey);
        review.setScoreGrade(dto.getScoreGrade());
        review.setComment(dto.getComment());
        review.setTaskDoneCount(hint.getTaskDoneCount());
        review.setTaskTotalCount(hint.getTaskTotalCount());
        review.setTaskAvgGrade(hint.getTaskAvgGrade());
        review.setReviewerId(reviewerId);
        boolean submit = Boolean.TRUE.equals(dto.getSubmit());
        review.setStatus(submit ? 1 : 0);
        reviewMapper.insert(review);
        return review.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PerformanceSaveDTO dto) {
        PerformanceReview review = requireReview(id);
        if (review.getStatus() != null && review.getStatus() == 2) {
            throw new BusinessException("已确认的考核单不可修改");
        }
        assertCanManageEmployee(review.getEmployeeId());
        if (!Objects.equals(review.getEmployeeId(), dto.getEmployeeId())
                || !Objects.equals(review.getPeriodType(), dto.getPeriodType())
                || !Objects.equals(review.getPeriodKey(), dto.getPeriodKey().trim())) {
            throw new BusinessException("不可变更员工或考核周期，请新建考核单");
        }

        LocalDateTime[] range = resolvePeriodRange(review.getPeriodType(), review.getPeriodKey());
        PerformanceTaskHintVO hint = buildTaskHint(
                review.getEmployeeId(), review.getPeriodType(), review.getPeriodKey(), range[0], range[1]);

        review.setScoreGrade(dto.getScoreGrade());
        review.setComment(dto.getComment());
        review.setTaskDoneCount(hint.getTaskDoneCount());
        review.setTaskTotalCount(hint.getTaskTotalCount());
        review.setTaskAvgGrade(hint.getTaskAvgGrade());
        review.setReviewerId(SecurityUtil.requireEmployeeId());
        if (Boolean.TRUE.equals(dto.getSubmit())) {
            review.setStatus(1);
        }
        reviewMapper.updateById(review);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        PerformanceReview review = requireReview(id);
        assertCanManageEmployee(review.getEmployeeId());
        if (review.getStatus() == null || review.getStatus() != 0) {
            throw new BusinessException("仅草稿可提交");
        }
        if (review.getScoreGrade() == null) {
            throw new BusinessException("请先完成评分");
        }
        review.setStatus(1);
        reviewMapper.updateById(review);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        PerformanceReview review = requireReview(id);
        if (!SecurityUtil.isHrStaff() && !SecurityUtil.isManagerUp()) {
            throw new BusinessException("无权确认考核");
        }
        assertCanManageEmployee(review.getEmployeeId());
        if (review.getStatus() == null || review.getStatus() != 1) {
            throw new BusinessException("仅已提交的考核单可确认");
        }
        review.setStatus(2);
        review.setConfirmedAt(LocalDateTime.now());
        review.setConfirmedBy(SecurityUtil.requireEmployeeId());
        reviewMapper.updateById(review);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        PerformanceReview review = requireReview(id);
        if (!SecurityUtil.isHrStaff()) {
            Long myId = SecurityUtil.requireEmployeeId();
            if (!Objects.equals(review.getReviewerId(), myId) || (review.getStatus() != null && review.getStatus() != 0)) {
                throw new BusinessException("仅可删除本人创建的草稿，或由人事删除");
            }
        }
        if (review.getStatus() != null && review.getStatus() == 2 && !SecurityUtil.isHrStaff()) {
            throw new BusinessException("已确认考核需人事处理");
        }
        reviewMapper.deleteById(id);
    }

    /**
     * null=不限制；empty=无权限数据
     */
    private Set<Long> resolveScopedEmployeeIds(Long deptId, Long employeeIdFilter) {
        if (SecurityUtil.isHrStaff()) {
            if (deptId == null) {
                return null;
            }
            return employeeMapper.selectList(
                            new LambdaQueryWrapper<Employee>().eq(Employee::getDeptId, deptId))
                    .stream().map(Employee::getId).collect(Collectors.toSet());
        }

        Long myId = SecurityUtil.requireEmployeeId();
        Employee me = employeeMapper.selectById(myId);
        Set<Long> ids = new HashSet<>();
        ids.add(myId);

        if (SecurityUtil.isManagerUp() && me != null && me.getDeptId() != null) {
            Long scopeDept = deptId != null ? deptId : me.getDeptId();
            if (deptId != null && !Objects.equals(deptId, me.getDeptId()) && !SecurityUtil.isHrStaff()) {
                return Set.of();
            }
            employeeMapper.selectList(
                            new LambdaQueryWrapper<Employee>().eq(Employee::getDeptId, scopeDept))
                    .forEach(e -> ids.add(e.getId()));
        }

        if (employeeIdFilter != null && !ids.contains(employeeIdFilter)) {
            return Set.of();
        }
        return ids;
    }

    private void assertCanView(Long employeeId) {
        Set<Long> scoped = resolveScopedEmployeeIds(null, employeeId);
        if (scoped != null && !scoped.contains(employeeId)) {
            throw new BusinessException("无权查看该员工考核");
        }
    }

    private void assertCanManageEmployee(Long employeeId) {
        Employee emp = employeeMapper.selectById(employeeId);
        if (emp == null) {
            throw new BusinessException("员工不存在");
        }
        if (SecurityUtil.isHrStaff()) {
            return;
        }
        if (!SecurityUtil.isManagerUp()) {
            throw new BusinessException("无权操作考核");
        }
        Long myId = SecurityUtil.requireEmployeeId();
        Employee me = employeeMapper.selectById(myId);
        if (me == null || me.getDeptId() == null || !Objects.equals(me.getDeptId(), emp.getDeptId())) {
            throw new BusinessException("只能考核本部门员工");
        }
    }

    private PerformanceReview requireReview(Long id) {
        if (id == null) {
            throw new BusinessException("考核单ID不能为空");
        }
        PerformanceReview review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException("考核单不存在");
        }
        return review;
    }

    private void validatePeriod(Integer periodType, String periodKey) {
        if (periodType == null || (periodType != 1 && periodType != 2)) {
            throw new BusinessException("周期类型无效");
        }
        if (!StringUtils.hasText(periodKey)) {
            throw new BusinessException("考核周期不能为空");
        }
        String key = periodKey.trim();
        if (periodType == 1 && !MONTH_KEY.matcher(key).matches()) {
            throw new BusinessException("月度周期格式应为 yyyy-MM，如 2026-08");
        }
        if (periodType == 2 && !QUARTER_KEY.matcher(key).matches()) {
            throw new BusinessException("季度周期格式应为 yyyy-Qn，如 2026-Q3");
        }
    }

    private LocalDateTime[] resolvePeriodRange(Integer periodType, String periodKey) {
        if (periodType == 1) {
            YearMonth ym = YearMonth.parse(periodKey);
            return new LocalDateTime[]{
                    ym.atDay(1).atStartOfDay(),
                    ym.plusMonths(1).atDay(1).atStartOfDay()
            };
        }
        int year = Integer.parseInt(periodKey.substring(0, 4));
        int q = Integer.parseInt(periodKey.substring(6));
        int startMonth = (q - 1) * 3 + 1;
        LocalDate start = LocalDate.of(year, startMonth, 1);
        return new LocalDateTime[]{start.atStartOfDay(), start.plusMonths(3).atStartOfDay()};
    }

    private PerformanceTaskHintVO buildTaskHint(
            Long employeeId, Integer periodType, String periodKey,
            LocalDateTime start, LocalDateTime endExclusive) {
        List<TaskAssignee> assignees = taskAssigneeMapper.selectList(
                new LambdaQueryWrapper<TaskAssignee>().eq(TaskAssignee::getEmployeeId, employeeId));
        PerformanceTaskHintVO vo = new PerformanceTaskHintVO();
        vo.setEmployeeId(employeeId);
        vo.setPeriodType(periodType);
        vo.setPeriodKey(periodKey);
        if (assignees.isEmpty()) {
            vo.setTaskDoneCount(0);
            vo.setTaskTotalCount(0);
            vo.setScoredCount(0);
            return vo;
        }
        Set<Long> taskIds = assignees.stream().map(TaskAssignee::getTaskId).collect(Collectors.toSet());
        Map<Long, Task> taskMap = taskMapper.selectBatchIds(taskIds).stream()
                .collect(Collectors.toMap(Task::getId, t -> t, (a, b) -> a));

        List<TaskAssignee> inPeriod = assignees.stream().filter(a -> {
            Task t = taskMap.get(a.getTaskId());
            if (t == null) {
                return false;
            }
            LocalDateTime ref = a.getFinishTime() != null ? a.getFinishTime()
                    : (t.getDueTime() != null ? t.getDueTime() : t.getCreatedAt());
            return ref != null && !ref.isBefore(start) && ref.isBefore(endExclusive);
        }).toList();

        int total = inPeriod.size();
        int done = (int) inPeriod.stream().filter(a -> a.getStatus() != null && a.getStatus() == 2).count();
        List<Integer> grades = inPeriod.stream()
                .map(TaskAssignee::getScoreGrade)
                .filter(Objects::nonNull)
                .toList();
        vo.setTaskTotalCount(total);
        vo.setTaskDoneCount(done);
        vo.setScoredCount(grades.size());
        if (!grades.isEmpty()) {
            double avg = grades.stream().mapToInt(Integer::intValue).average().orElse(0);
            vo.setTaskAvgGrade(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        }
        return vo;
    }

    private List<PerformanceReviewVO> toVos(List<PerformanceReview> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return List.of();
        }
        Set<Long> empIds = new HashSet<>();
        reviews.forEach(r -> {
            if (r.getEmployeeId() != null) {
                empIds.add(r.getEmployeeId());
            }
            if (r.getReviewerId() != null) {
                empIds.add(r.getReviewerId());
            }
        });
        Map<Long, Employee> empMap = empIds.isEmpty() ? Map.of()
                : employeeMapper.selectBatchIds(empIds).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
        Set<Long> deptIds = empMap.values().stream()
                .map(Employee::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> deptNames = deptIds.isEmpty() ? Map.of()
                : departmentMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName, (a, b) -> a));

        return reviews.stream().map(r -> {
            PerformanceReviewVO vo = new PerformanceReviewVO();
            vo.setId(r.getId());
            vo.setEmployeeId(r.getEmployeeId());
            Employee emp = empMap.get(r.getEmployeeId());
            if (emp != null) {
                vo.setEmployeeName(emp.getName());
                vo.setEmpNo(emp.getEmpNo());
                vo.setDeptId(emp.getDeptId());
                vo.setDeptName(deptNames.get(emp.getDeptId()));
            }
            vo.setPeriodType(r.getPeriodType());
            vo.setPeriodKey(r.getPeriodKey());
            vo.setScoreGrade(r.getScoreGrade());
            vo.setScoreGradeLabel(r.getScoreGrade() == null
                    ? null : GRADE_LABEL.getOrDefault(r.getScoreGrade(), String.valueOf(r.getScoreGrade())));
            vo.setComment(r.getComment());
            vo.setTaskDoneCount(r.getTaskDoneCount());
            vo.setTaskTotalCount(r.getTaskTotalCount());
            vo.setTaskAvgGrade(r.getTaskAvgGrade());
            vo.setReviewerId(r.getReviewerId());
            Employee reviewer = empMap.get(r.getReviewerId());
            vo.setReviewerName(reviewer != null ? reviewer.getName() : null);
            vo.setStatus(r.getStatus());
            vo.setStatusLabel(STATUS_LABEL.getOrDefault(r.getStatus(), String.valueOf(r.getStatus())));
            vo.setConfirmedAt(r.getConfirmedAt());
            vo.setCreatedAt(r.getCreatedAt());
            vo.setUpdatedAt(r.getUpdatedAt());
            return vo;
        }).toList();
    }
}
