package org.example.hrmanagement.module.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.department.entity.Department;
import org.example.hrmanagement.module.department.mapper.DepartmentMapper;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.notification.constant.NotificationBizType;
import org.example.hrmanagement.module.notification.service.NotificationService;
import org.example.hrmanagement.module.task.constant.TaskHallConstants;
import org.example.hrmanagement.module.task.dto.TaskHallAbandonDTO;
import org.example.hrmanagement.module.task.dto.TaskHallCreateDTO;
import org.example.hrmanagement.module.task.dto.TaskHallReclaimDTO;
import org.example.hrmanagement.module.task.entity.Task;
import org.example.hrmanagement.module.task.entity.TaskAssignee;
import org.example.hrmanagement.module.task.entity.TaskHallDeduct;
import org.example.hrmanagement.module.task.entity.TaskLog;
import org.example.hrmanagement.module.task.mapper.TaskAssigneeMapper;
import org.example.hrmanagement.module.task.mapper.TaskHallDeductMapper;
import org.example.hrmanagement.module.task.mapper.TaskLogMapper;
import org.example.hrmanagement.module.task.mapper.TaskMapper;
import org.example.hrmanagement.module.task.service.TaskHallService;
import org.example.hrmanagement.module.task.vo.TaskHallClaimResultVO;
import org.example.hrmanagement.module.task.vo.TaskVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskHallServiceImpl implements TaskHallService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final TaskMapper taskMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;
    private final TaskHallDeductMapper taskHallDeductMapper;
    private final TaskLogMapper taskLogMapper;
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(TaskHallCreateDTO dto) {
        if (!SecurityUtil.hasAnyRole("HR_ADMIN", "DEPT_MANAGER")) {
            throw new BusinessException("仅 HR 或部门经理可发布大厅任务");
        }
        validatePolicy(dto.getOverduePolicy(), dto.getDeductAmount());

        Long creatorId = SecurityUtil.requireEmployeeId();
        Employee creator = employeeMapper.selectById(creatorId);
        if (creator == null) {
            throw new BusinessException("创建人信息不存在");
        }

        Long deptId;
        if (SecurityUtil.hasRole("DEPT_MANAGER") && !SecurityUtil.isHrStaff()) {
            deptId = creator.getDeptId();
            if (deptId == null) {
                throw new BusinessException("当前账号未关联部门");
            }
        } else {
            deptId = dto.getDeptId() != null ? dto.getDeptId() : creator.getDeptId();
            if (deptId == null) {
                throw new BusinessException("请指定发布部门");
            }
        }

        Task task = new Task();
        task.setTitle(dto.getTitle().trim());
        task.setContent(dto.getContent());
        task.setParentId(0L);
        task.setCreatorId(creatorId);
        task.setDeptId(deptId);
        task.setPriority(2);
        task.setClaimMode(TaskHallConstants.CLAIM_MODE_OPEN);
        task.setClaimQuota(dto.getClaimQuota());
        task.setDifficulty(dto.getDifficulty());
        task.setSuggestBonus(dto.getSuggestBonus());
        task.setOverduePolicy(dto.getOverduePolicy().trim().toUpperCase());
        task.setDeductAmount(dto.getDeductAmount());
        task.setClaimedCount(0);
        task.setVersion(0);
        task.setStatus(0);
        task.setDueTime(dto.getDueTime());
        taskMapper.insert(task);

        saveLog(task.getId(), creatorId, "CREATE",
                "发布大厅任务，名额 " + dto.getClaimQuota()
                        + "，难度 " + dto.getDifficulty()
                        + "，策略 " + task.getOverduePolicy());

        notifyDeptEmployees(deptId, creatorId,
                "部门任务大厅新任务：" + task.getTitle(),
                buildHallBrief(task));
    }

    @Override
    public PageResult<TaskVO> listOpen(PageQuery page) {
        // HR / 超管看全部部门；经理与员工仅本部门
        Long deptId = SecurityUtil.isHrStaff() ? null : resolveViewDeptId();
        Page<Task> mpPage = new Page<>(page.getPageNum(), page.getPageSize());
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getClaimMode, TaskHallConstants.CLAIM_MODE_OPEN)
                .eq(Task::getStatus, 0)
                .apply("claimed_count < claim_quota")
                .orderByDesc(Task::getId);
        if (deptId != null) {
            wrapper.eq(Task::getDeptId, deptId);
        }
        IPage<Task> result = taskMapper.selectPage(mpPage, wrapper);

        Set<Long> creatorIds = result.getRecords().stream()
                .map(Task::getCreatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> deptIds = result.getRecords().stream()
                .map(Task::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> names = loadNames(creatorIds);
        Map<Long, String> deptNames = loadDeptNames(deptIds);
        LocalDateTime now = LocalDateTime.now();
        List<TaskVO> records = result.getRecords().stream()
                .map(t -> toHallVO(t, names.get(t.getCreatorId()), deptNames.get(t.getDeptId()), now))
                .toList();
        PageResult<TaskVO> pageResult = new PageResult<>();
        pageResult.setRecords(records);
        pageResult.setTotal(result.getTotal());
        pageResult.setPageNum(result.getCurrent());
        pageResult.setPageSize(result.getSize());
        pageResult.setPages(result.getPages());
        return pageResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskHallClaimResultVO claim(Long taskId) {
        // 登录角色以 JWT 中本次选择的角色为准：仅 EMPLOYEE 可接
        if (!SecurityUtil.hasRole("EMPLOYEE")) {
            throw new BusinessException("仅普通员工可从大厅接取任务");
        }

        Long empId = SecurityUtil.requireEmployeeId();
        Employee me = employeeMapper.selectById(empId);
        if (me == null || me.getDeptId() == null) {
            throw new BusinessException("当前账号未关联部门");
        }

        Task task = taskMapper.selectByIdForUpdate(taskId);
        if (task == null) {
            throw new BusinessException("该任务不存在");
        }
        if (!TaskHallConstants.CLAIM_MODE_OPEN.equals(task.getClaimMode())) {
            throw new BusinessException("非大厅任务，无法接取");
        }
        if (task.getStatus() == null || task.getStatus() != 0) {
            throw new BusinessException("任务当前不可接取");
        }
        if (!Objects.equals(task.getDeptId(), me.getDeptId())) {
            throw new BusinessException("仅可接取本部门任务");
        }

        long active = countActiveClaimers(taskId);
        if (active >= safeQuota(task)) {
            throw new BusinessException("任务名额已满");
        }
        if (hasActiveClaim(taskId, empId)) {
            throw new BusinessException("您已接取该任务");
        }

        TaskAssignee assignee = new TaskAssignee();
        assignee.setTaskId(taskId);
        assignee.setEmployeeId(empId);
        assignee.setSource(TaskHallConstants.SOURCE_CLAIM);
        assignee.setStatus(1);
        assignee.setProgress(0);
        assignee.setAcceptTime(LocalDateTime.now());
        try {
            taskAssigneeMapper.insert(assignee);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("您已接取该任务");
        }

        int cnt = (int) countActiveClaimers(taskId);
        task.setClaimedCount(cnt);
        if (cnt >= safeQuota(task)) {
            task.setStatus(1);
        }
        task.setVersion(task.getVersion() == null ? 1 : task.getVersion() + 1);
        taskMapper.updateById(task);

        saveLog(taskId, empId, "CLAIM", "接取大厅任务");
        if (task.getCreatorId() != null) {
            notificationService.sendToEmployees(
                    List.of(task.getCreatorId()),
                    "任务被接取：" + task.getTitle(),
                    me.getName() + " 已接取（" + cnt + "/" + safeQuota(task) + "）",
                    NotificationBizType.TASK_ASSIGN,
                    taskId,
                    NotificationBizType.DEFAULT_TASK_LINK);
        }

        TaskHallClaimResultVO vo = new TaskHallClaimResultVO();
        vo.setTaskId(taskId);
        vo.setClaimedCount(cnt);
        vo.setClaimQuota(safeQuota(task));
        vo.setTaskStatus(task.getStatus());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void abandon(Long taskId, TaskHallAbandonDTO dto) {
        Long empId = SecurityUtil.requireEmployeeId();
        Task task = taskMapper.selectByIdForUpdate(taskId);
        requireOpenMode(task);

        TaskAssignee assignee = findActiveClaim(taskId, empId);
        if (assignee == null) {
            throw new BusinessException("仅接取人可放弃");
        }
        if (assignee.getStatus() != null && assignee.getStatus() == 2) {
            throw new BusinessException("已完成任务不可放弃");
        }
        assignee.setStatus(4);
        assignee.setRejectReason(dto.getReason());
        taskAssigneeMapper.updateById(assignee);

        refreshClaimCountAndMaybeReopen(task);
        saveLog(taskId, empId, "ABANDON", dto.getReason());
        if (task.getCreatorId() != null) {
            Employee me = employeeMapper.selectById(empId);
            notificationService.sendToEmployees(
                    List.of(task.getCreatorId()),
                    "任务被放弃：" + task.getTitle(),
                    (me != null ? me.getName() : "执行人") + "：" + dto.getReason(),
                    NotificationBizType.TASK_REJECT,
                    taskId,
                    NotificationBizType.DEFAULT_TASK_LINK);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reclaim(Long taskId, TaskHallReclaimDTO dto) {
        Task task = taskMapper.selectByIdForUpdate(taskId);
        requireOpenMode(task);
        Long operatorId = SecurityUtil.requireEmployeeId();
        assertCanReclaim(task);

        if (task.getStatus() != null && task.getStatus() == 3) {
            throw new BusinessException("当前状态不可收回");
        }

        if (dto.getEmployeeId() != null) {
            TaskAssignee a = findActiveClaim(taskId, dto.getEmployeeId());
            if (a == null) {
                throw new BusinessException("该执行人未在任务中");
            }
            a.setStatus(4);
            a.setRejectReason(dto.getReason());
            taskAssigneeMapper.updateById(a);
            refreshClaimCountAndMaybeReopen(task);
            saveLog(taskId, operatorId, "RECLAIM", "收回执行人 " + dto.getEmployeeId() + "：" + dto.getReason());
            return;
        }

        String action = StringUtils.hasText(dto.getAction())
                ? dto.getAction().trim().toUpperCase()
                : TaskHallConstants.RECLAIM_BACK_TO_HALL;

        if (TaskHallConstants.RECLAIM_CLOSE.equals(action)) {
            closeHallTask(task, operatorId, dto.getReason());
            return;
        }

        List<TaskAssignee> actives = listActiveClaimers(taskId);
        for (TaskAssignee a : actives) {
            a.setStatus(4);
            a.setRejectReason(dto.getReason());
            taskAssigneeMapper.updateById(a);
        }
        task.setClaimedCount(0);
        task.setStatus(0);
        task.setVersion(task.getVersion() == null ? 1 : task.getVersion() + 1);
        taskMapper.updateById(task);
        saveLog(taskId, operatorId, "RECLAIM", "整单收回回大厅：" + dto.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyClosePolicy(Task task, Long operatorEmpId, String reason) {
        if (task == null || !TaskHallConstants.CLAIM_MODE_OPEN.equals(task.getClaimMode())) {
            return;
        }
        String policy = task.getOverduePolicy();
        if (!StringUtils.hasText(policy)) {
            return;
        }
        policy = policy.trim().toUpperCase();
        List<TaskAssignee> unfinished = taskAssigneeMapper.selectList(
                new LambdaQueryWrapper<TaskAssignee>()
                        .eq(TaskAssignee::getTaskId, task.getId())
                        .eq(TaskAssignee::getSource, TaskHallConstants.SOURCE_CLAIM)
                        .in(TaskAssignee::getStatus, List.of(0, 1, 4)));

        if (TaskHallConstants.POLICY_ZERO_BONUS.equals(policy)) {
            for (TaskAssignee a : unfinished) {
                a.setScoreBonus(BigDecimal.ZERO);
                taskAssigneeMapper.updateById(a);
            }
        } else if (TaskHallConstants.POLICY_DEDUCT.equals(policy)) {
            BigDecimal amount = task.getDeductAmount();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }
            String month = YearMonth.now().format(MONTH_FMT);
            for (TaskAssignee a : unfinished) {
                if (a.getStatus() != null && a.getStatus() == 2) {
                    continue;
                }
                // 仅未完成（含关闭前被标 4 的进行中）
                if (a.getStatus() != null && a.getStatus() == 2) {
                    continue;
                }
                Long exists = taskHallDeductMapper.selectCount(
                        new LambdaQueryWrapper<TaskHallDeduct>()
                                .eq(TaskHallDeduct::getTaskId, task.getId())
                                .eq(TaskHallDeduct::getEmployeeId, a.getEmployeeId()));
                if (exists != null && exists > 0) {
                    continue;
                }
                TaskHallDeduct row = new TaskHallDeduct();
                row.setTaskId(task.getId());
                row.setEmployeeId(a.getEmployeeId());
                row.setAmount(amount);
                row.setReason(StringUtils.hasText(reason) ? reason : "大厅任务未完成扣款");
                row.setDeductMonth(month);
                row.setCreatedAt(LocalDateTime.now());
                row.setCreatedBy(operatorEmpId);
                try {
                    taskHallDeductMapper.insert(row);
                } catch (DuplicateKeyException ignored) {
                    // 幂等
                }
            }
        }
        // MARK_ONLY: 无额外动作
    }

    private void closeHallTask(Task task, Long operatorId, String reason) {
        if (task.getStatus() != null && task.getStatus() == 3) {
            throw new BusinessException("任务已关闭");
        }
        List<TaskAssignee> assignees = listActiveClaimers(task.getId());
        for (TaskAssignee a : assignees) {
            if (a.getStatus() != null && (a.getStatus() == 0 || a.getStatus() == 1)) {
                a.setStatus(4);
                taskAssigneeMapper.updateById(a);
            }
        }
        applyClosePolicy(task, operatorId, reason);
        task.setStatus(3);
        task.setVersion(task.getVersion() == null ? 1 : task.getVersion() + 1);
        taskMapper.updateById(task);
        saveLog(task.getId(), operatorId, "CLOSE", "强制关闭大厅任务：" + reason);
    }

    private void refreshClaimCountAndMaybeReopen(Task task) {
        int cnt = (int) countActiveClaimers(task.getId());
        task.setClaimedCount(cnt);
        int quota = safeQuota(task);
        if (task.getStatus() != null && task.getStatus() != 2 && task.getStatus() != 3) {
            task.setStatus(cnt >= quota ? 1 : 0);
        }
        task.setVersion(task.getVersion() == null ? 1 : task.getVersion() + 1);
        taskMapper.updateById(task);
    }

    private void assertCanReclaim(Task task) {
        if (SecurityUtil.isHrStaff()) {
            return;
        }
        if (SecurityUtil.hasRole("DEPT_MANAGER")) {
            Long deptId = SecurityUtil.getDeptId();
            if (deptId != null && Objects.equals(deptId, task.getDeptId())) {
                return;
            }
        }
        throw new BusinessException("无权收回该任务");
    }

    private Long resolveViewDeptId() {
        if (SecurityUtil.isHrStaff()) {
            Long deptId = SecurityUtil.getDeptId();
            if (deptId == null) {
                throw new BusinessException("请先绑定部门后再查看大厅");
            }
            return deptId;
        }
        return SecurityUtil.requireDeptId();
    }

    private void validatePolicy(String policy, BigDecimal deductAmount) {
        if (!StringUtils.hasText(policy)) {
            throw new BusinessException("逾期策略不能为空");
        }
        String p = policy.trim().toUpperCase();
        if (!Set.of(
                TaskHallConstants.POLICY_MARK_ONLY,
                TaskHallConstants.POLICY_ZERO_BONUS,
                TaskHallConstants.POLICY_DEDUCT).contains(p)) {
            throw new BusinessException("逾期策略无效");
        }
        if (TaskHallConstants.POLICY_DEDUCT.equals(p)
                && (deductAmount == null || deductAmount.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessException("请填写扣款金额");
        }
    }

    private void requireOpenMode(Task task) {
        if (task == null) {
            throw new BusinessException("该任务不存在");
        }
        if (!TaskHallConstants.CLAIM_MODE_OPEN.equals(task.getClaimMode())) {
            throw new BusinessException("非大厅任务");
        }
    }

    private int safeQuota(Task task) {
        Integer q = task.getClaimQuota();
        return q == null || q < 1 ? 1 : q;
    }

    private long countActiveClaimers(Long taskId) {
        Long cnt = taskAssigneeMapper.selectCount(new LambdaQueryWrapper<TaskAssignee>()
                .eq(TaskAssignee::getTaskId, taskId)
                .eq(TaskAssignee::getSource, TaskHallConstants.SOURCE_CLAIM)
                .in(TaskAssignee::getStatus, List.of(0, 1, 2)));
        return cnt == null ? 0 : cnt;
    }

    private boolean hasActiveClaim(Long taskId, Long empId) {
        return findActiveClaim(taskId, empId) != null;
    }

    private TaskAssignee findActiveClaim(Long taskId, Long empId) {
        return taskAssigneeMapper.selectOne(new LambdaQueryWrapper<TaskAssignee>()
                .eq(TaskAssignee::getTaskId, taskId)
                .eq(TaskAssignee::getEmployeeId, empId)
                .eq(TaskAssignee::getSource, TaskHallConstants.SOURCE_CLAIM)
                .in(TaskAssignee::getStatus, List.of(0, 1, 2))
                .last("LIMIT 1"));
    }

    private List<TaskAssignee> listActiveClaimers(Long taskId) {
        return taskAssigneeMapper.selectList(new LambdaQueryWrapper<TaskAssignee>()
                .eq(TaskAssignee::getTaskId, taskId)
                .eq(TaskAssignee::getSource, TaskHallConstants.SOURCE_CLAIM)
                .in(TaskAssignee::getStatus, List.of(0, 1, 2)));
    }

    private void notifyDeptEmployees(Long deptId, Long excludeEmpId, String title, String content) {
        List<Employee> emps = employeeMapper.selectList(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getDeptId, deptId)
                .in(Employee::getStatus, List.of(1, 2)));
        List<Long> ids = emps.stream()
                .map(Employee::getId)
                .filter(id -> !Objects.equals(id, excludeEmpId))
                .toList();
        if (!ids.isEmpty()) {
            notificationService.sendToEmployees(
                    ids, title, content,
                    NotificationBizType.TASK_ASSIGN, null,
                    "/hr/task-hall");
        }
    }

    private String buildHallBrief(Task task) {
        return "难度 " + task.getDifficulty() + " 星，名额 "
                + task.getClaimQuota() + " 人，请到任务大厅查看";
    }

    private TaskVO toHallVO(Task task, String creatorName, String deptName, LocalDateTime now) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setTitle(task.getTitle());
        vo.setContent(task.getContent());
        vo.setPriority(task.getPriority());
        vo.setStatus(task.getStatus());
        vo.setDueTime(task.getDueTime());
        vo.setCreatorId(task.getCreatorId());
        vo.setCreatorName(creatorName);
        vo.setDeptId(task.getDeptId());
        vo.setDeptName(deptName);
        vo.setClaimMode(task.getClaimMode());
        vo.setClaimQuota(task.getClaimQuota());
        vo.setClaimedCount(task.getClaimedCount());
        vo.setDifficulty(task.getDifficulty());
        vo.setSuggestBonus(task.getSuggestBonus());
        vo.setOverduePolicy(task.getOverduePolicy());
        vo.setDeductAmount(task.getDeductAmount());
        vo.setOverdue(task.getDueTime() != null
                && task.getDueTime().isBefore(now)
                && task.getStatus() != null
                && (task.getStatus() == 0 || task.getStatus() == 1));
        return vo;
    }

    private Map<Long, String> loadNames(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return employeeMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getName, (a, b) -> a));
    }

    private Map<Long, String> loadDeptNames(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return departmentMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName, (a, b) -> a));
    }

    private void saveLog(Long taskId, Long operatorId, String action, String remark) {
        TaskLog log = new TaskLog();
        log.setTaskId(taskId);
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        taskLogMapper.insert(log);
    }
}
