package org.example.hrmanagement.module.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.notification.constant.NotificationBizType;
import org.example.hrmanagement.module.notification.service.NotificationService;
import org.example.hrmanagement.module.salary.entity.TaskScoreBonusDict;
import org.example.hrmanagement.module.salary.mapper.TaskScoreBonusDictMapper;
import org.example.hrmanagement.module.task.dto.TaskCreateDto;
import org.example.hrmanagement.module.task.dto.TaskProgressDTO;
import org.example.hrmanagement.module.task.dto.TaskRejectDTO;
import org.example.hrmanagement.module.task.dto.TaskScoreDTO;
import org.example.hrmanagement.module.task.entity.Task;
import org.example.hrmanagement.module.task.entity.TaskAssignee;
import org.example.hrmanagement.module.task.entity.TaskLog;
import org.example.hrmanagement.module.task.mapper.TaskAssigneeMapper;
import org.example.hrmanagement.module.task.mapper.TaskLogMapper;
import org.example.hrmanagement.module.task.mapper.TaskMapper;
import org.example.hrmanagement.module.task.service.TaskAttachmentService;
import org.example.hrmanagement.module.task.service.TaskService;
import org.example.hrmanagement.module.task.vo.TaskAssigneeVO;
import org.example.hrmanagement.module.task.vo.TaskBoardVO;
import org.example.hrmanagement.module.task.vo.TaskDetailVO;
import org.example.hrmanagement.module.task.vo.TaskLogVO;
import org.example.hrmanagement.module.task.vo.TaskTodoStatsVO;
import org.example.hrmanagement.module.task.vo.TaskVO;
import org.example.hrmanagement.module.project.entity.Project;
import org.example.hrmanagement.module.project.mapper.ProjectMapper;
import org.example.hrmanagement.module.project.service.ProjectService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 任务业务实现。
 */
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private static final Map<Integer, String> SCORE_GRADE_LABEL = Map.of(
            1, "优",
            2, "良",
            3, "中",
            4, "合格",
            5, "差"
    );

    private final EmployeeMapper employeeMapper;
    private final TaskMapper taskMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;
    private final TaskLogMapper taskLogMapper;
    private final NotificationService notificationService;
    private final TaskAttachmentService taskAttachmentService;
    private final TaskScoreBonusDictMapper taskScoreBonusDictMapper;
    private final ProjectMapper projectMapper;
    private final ObjectProvider<ProjectService> projectServiceProvider;

    /**
     * 创建并下发任务：校验执行人 → 写 hr_task / hr_task_assignee → CREATE 日志。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(TaskCreateDto dto) {
        Long creatorId = SecurityUtil.requireEmployeeId();
        Employee creator = employeeMapper.selectById(creatorId);
        if (creator == null) {
            throw new BusinessException("创建人信息不存在");
        }

        Set<Long> assigneeIds = new LinkedHashSet<>(dto.getAssigneeIds());
        if (assigneeIds.isEmpty()) {
            throw new BusinessException("请选择执行人");
        }

        boolean deptManagerOnly = SecurityUtil.hasRole("DEPT_MANAGER") && !SecurityUtil.isHrStaff();
        Long creatorDeptId = creator.getDeptId();

        for (Long empId : assigneeIds) {
            Employee assignee = employeeMapper.selectById(empId);
            if (assignee == null) {
                throw new BusinessException("执行人不存在：" + empId);
            }
            // 1-在职 2-试用期 可指派；3-离职不可
            if (assignee.getStatus() == null || assignee.getStatus() == 3) {
                throw new BusinessException("不能指派给已离职员工：" + assignee.getName());
            }
            if (deptManagerOnly && (assignee.getDeptId() == null || !assignee.getDeptId().equals(creatorDeptId))) {
                throw new BusinessException("只能向本部门员工下发任务：" + assignee.getName());
            }
        }

        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        Long projectId = dto.getProjectId();
        if (parentId > 0) {
            Task parent = taskMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException("父任务不存在");
            }
            if (parent.getParentId() != null && parent.getParentId() > 0) {
                throw new BusinessException("仅支持一层子任务，请挂到根任务下");
            }
            if (parent.getStatus() != null && parent.getStatus() == 3) {
                throw new BusinessException("父任务已关闭，无法添加子任务");
            }
            if (projectId == null) {
                projectId = parent.getProjectId();
            }
            if (parent.getStatus() != null && parent.getStatus() == 0) {
                parent.setStatus(1);
                taskMapper.updateById(parent);
            }
        }
        if (projectId != null) {
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                throw new BusinessException("所属项目不存在");
            }
            if (project.getStatus() != null && project.getStatus() == 3) {
                throw new BusinessException("项目已关闭，无法挂接任务");
            }
        }

        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setContent(dto.getContent());
        task.setParentId(parentId);
        task.setProjectId(projectId);
        task.setCreatorId(creatorId);
        task.setDeptId(creatorDeptId);
        task.setPriority(dto.getPriority() == null ? 2 : dto.getPriority());
        task.setStatus(0);
        task.setStartTime(dto.getStartTime());
        task.setDueTime(dto.getDueTime());
        taskMapper.insert(task);

        for (Long empId : assigneeIds) {
            TaskAssignee assignee = new TaskAssignee();
            assignee.setTaskId(task.getId());
            assignee.setEmployeeId(empId);
            assignee.setStatus(0);
            assignee.setProgress(0);
            taskAssigneeMapper.insert(assignee);
        }

        saveLog(task.getId(), creatorId, "CREATE",
                parentId > 0
                        ? "创建子任务并下发，执行人 " + assigneeIds.size() + " 人"
                        : "创建任务并下发，执行人 " + assigneeIds.size() + " 人");
        notificationService.sendToEmployees(
                assigneeIds,
                "您有新任务：" + task.getTitle(),
                StringUtils.hasText(task.getContent()) ? task.getContent() : "请及时接收并处理",
                NotificationBizType.TASK_ASSIGN,
                task.getId(),
                NotificationBizType.DEFAULT_TASK_LINK);

        if (parentId > 0) {
            refreshParentFromChildren(parentId);
        }
        notifyProjectProgress(projectId);
    }

    /**
     * 分页列表：mine 按执行人关联查任务；created 按创建人查；组装逾期与个人进度。
     */
    @Override
    public PageResult<TaskVO> listTasks(String scope, Integer status, PageQuery pageQuery) {
        Long myEmployeeId = SecurityUtil.requireEmployeeId();
        String resolvedScope = StringUtils.hasText(scope) ? scope : "mine";

        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        Map<Long, TaskAssignee> myAssigneeMap = Map.of();

        if ("created".equalsIgnoreCase(resolvedScope)) {
            if (SecurityUtil.hasRole("EMPLOYEE") && !SecurityUtil.isManagerUp()) {
                throw new BusinessException("普通员工无法查看「我创建的」任务");
            }
            wrapper.eq(Task::getCreatorId, myEmployeeId);
        } else {
            List<TaskAssignee> myAssignees = taskAssigneeMapper.selectList(
                    new LambdaQueryWrapper<TaskAssignee>()
                            .eq(TaskAssignee::getEmployeeId, myEmployeeId));
            if (myAssignees.isEmpty()) {
                return PageResult.empty();
            }
            List<Long> taskIds = myAssignees.stream()
                    .map(TaskAssignee::getTaskId)
                    .distinct()
                    .toList();
            wrapper.in(Task::getId, taskIds);
            myAssigneeMap = myAssignees.stream()
                    .collect(Collectors.toMap(TaskAssignee::getTaskId, Function.identity(), (a, b) -> a));
        }

        if (status != null) {
            wrapper.eq(Task::getStatus, status);
        }
        wrapper.orderByDesc(Task::getCreatedAt);

        IPage<Task> iPage = taskMapper.selectPage(
                new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()), wrapper);
        List<Task> tasks = iPage.getRecords();
        if (tasks == null || tasks.isEmpty()) {
            return PageResult.empty();
        }

        Set<Long> creatorIds = tasks.stream()
                .map(Task::getCreatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> creatorNames = loadEmployeeNames(creatorIds);
        List<Long> taskIds = tasks.stream().map(Task::getId).toList();
        Map<Long, Integer> progressMap = loadAvgProgressByTaskIds(taskIds);
        Set<Long> parentsWithChildren = loadParentIdsHavingChildren(taskIds);

        LocalDateTime now = LocalDateTime.now();
        Map<Long, TaskAssignee> finalMyAssigneeMap = myAssigneeMap;
        List<TaskVO> vos = tasks.stream()
                .map(task -> toTaskVO(task, creatorNames.get(task.getCreatorId()),
                        finalMyAssigneeMap.get(task.getId()),
                        progressMap.getOrDefault(task.getId(), 0),
                        parentsWithChildren.contains(task.getId()),
                        now))
                .toList();

        PageResult<TaskVO> result = new PageResult<>();
        result.setRecords(vos);
        result.setTotal(iPage.getTotal());
        result.setPageNum(iPage.getCurrent());
        result.setPageSize(iPage.getSize());
        result.setPages(iPage.getPages());
        return result;
    }

    /**
     * 接收任务：仅待接收可操作；更新执行人状态与接收时间，主任务待接收时改为进行中。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("该任务不存在");
        }
        TaskAssignee a = getMyAssignee(taskId);
        if (a.getStatus() == null || a.getStatus() != 0) {
            throw new BusinessException("当前任务不可接收");
        }
        a.setStatus(1);
        a.setAcceptTime(LocalDateTime.now());
        taskAssigneeMapper.updateById(a);

        if (task.getStatus() == null || task.getStatus() == 0) {
            task.setStatus(1);
            taskMapper.updateById(task);
        }

        saveLog(taskId, a.getEmployeeId(), "ACCEPT", "接收任务");
    }

    /**
     * 更新进度：须已接收且新进度更高；达 100% 自动完成并汇总主任务状态；返回详情。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskDetailVO progress(Long taskId, TaskProgressDTO dto) {
        Task task = requireTask(taskId);
        if (task.getStatus() != null && task.getStatus() == 3) {
            throw new BusinessException("任务已关闭，无法更新进度");
        }

        TaskAssignee a = getMyAssignee(taskId);
        if (a.getStatus() == null || a.getStatus() != 1) {
            throw new BusinessException("请先接收任务后再更新进度");
        }

        int oldProgress = a.getProgress() == null ? 0 : a.getProgress();
        int newProgress = dto.getProgress();
        if (newProgress <= oldProgress) {
            throw new BusinessException("任务进度必须高于当前进度");
        }

        a.setProgress(newProgress);
        a.setFeedback(dto.getFeedback() == null ? "" : dto.getFeedback());

        boolean finished = newProgress == 100;
        if (finished) {
            a.setStatus(2);
            a.setFinishTime(LocalDateTime.now());
        }
        taskAssigneeMapper.updateById(a);

        if (finished) {
            refreshTaskStatus(taskId);
            saveLog(taskId, a.getEmployeeId(), "FINISH", "进度达到 100%，任务完成");
        } else {
            if (task.getStatus() == null || task.getStatus() == 0) {
                task.setStatus(1);
                taskMapper.updateById(task);
            }
            saveLog(taskId, a.getEmployeeId(), "PROGRESS", "进度更新为 " + newProgress + "%");
        }

        if (task.getParentId() != null && task.getParentId() > 0) {
            refreshParentFromChildren(task.getParentId());
        }
        notifyProjectProgress(task.getProjectId());
        return getDetail(taskId);
    }

    /**
     * 任务详情：权限校验后加载执行人、日志并填充姓名。
     */
    @Override
    public TaskDetailVO getDetail(Long taskId) {
        Task task = requireTask(taskId);
        Long myId = SecurityUtil.requireEmployeeId();

        List<TaskAssignee> assignees = taskAssigneeMapper.selectList(
                new LambdaQueryWrapper<TaskAssignee>().eq(TaskAssignee::getTaskId, taskId));
        boolean isAssignee = assignees.stream().anyMatch(a -> Objects.equals(a.getEmployeeId(), myId));
        boolean isCreator = Objects.equals(task.getCreatorId(), myId);
        if (!SecurityUtil.isHrStaff() && !isCreator && !isAssignee) {
            throw new BusinessException("无权查看该任务");
        }

        List<TaskLog> logs = taskLogMapper.selectList(
                new LambdaQueryWrapper<TaskLog>()
                        .eq(TaskLog::getTaskId, taskId)
                        .orderByAsc(TaskLog::getCreatedAt));

        Set<Long> empIds = new HashSet<>();
        if (task.getCreatorId() != null) {
            empIds.add(task.getCreatorId());
        }
        assignees.forEach(a -> {
            if (a.getEmployeeId() != null) {
                empIds.add(a.getEmployeeId());
            }
        });
        logs.forEach(l -> {
            if (l.getOperatorId() != null) {
                empIds.add(l.getOperatorId());
            }
        });
        assignees.forEach(a -> {
            if (a.getScoredBy() != null) {
                empIds.add(a.getScoredBy());
            }
        });
        Map<Long, String> nameMap = new java.util.HashMap<>(loadEmployeeNames(empIds));

        TaskAssignee myAssignee = assignees.stream()
                .filter(a -> Objects.equals(a.getEmployeeId(), myId))
                .findFirst()
                .orElse(null);

        List<Task> children = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getParentId, taskId)
                        .orderByAsc(Task::getId));
        if (!children.isEmpty()) {
            Set<Long> childCreatorIds = children.stream()
                    .map(Task::getCreatorId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            nameMap.putAll(loadEmployeeNames(childCreatorIds));
        }
        LocalDateTime now = LocalDateTime.now();
        Map<Long, Integer> childProgressMap = loadAvgProgressByTaskIds(
                children.stream().map(Task::getId).toList());
        List<TaskVO> childVos = children.stream()
                .map(c -> toTaskVO(c, nameMap.get(c.getCreatorId()),
                        null,
                        childProgressMap.getOrDefault(c.getId(), 0),
                        false,
                        now))
                .toList();

        TaskDetailVO vo = new TaskDetailVO();
        vo.setId(task.getId());
        vo.setTitle(task.getTitle());
        vo.setContent(task.getContent());
        vo.setParentId(task.getParentId());
        vo.setProjectId(task.getProjectId());
        vo.setPriority(task.getPriority());
        vo.setStatus(task.getStatus());
        vo.setStartTime(task.getStartTime());
        vo.setDueTime(task.getDueTime());
        vo.setCreatorId(task.getCreatorId());
        vo.setCreatorName(nameMap.get(task.getCreatorId()));
        vo.setOverdue(isOverdue(task, now));
        if (myAssignee != null) {
            vo.setMyStatus(myAssignee.getStatus());
            vo.setMyProgress(myAssignee.getProgress());
        }
        int avgProgress;
        if (!childVos.isEmpty()) {
            avgProgress = (int) Math.round(childVos.stream()
                    .mapToInt(c -> c.getProgress() == null ? 0 : c.getProgress())
                    .average()
                    .orElse(0));
        } else {
            avgProgress = assignees.isEmpty()
                    ? 0
                    : (int) Math.round(assignees.stream()
                    .mapToInt(a -> a.getProgress() == null ? 0 : a.getProgress())
                    .average()
                    .orElse(0));
        }
        vo.setProgress(avgProgress);
        vo.setChildren(childVos);
        vo.setAssignees(assignees.stream().map(a -> {
            TaskAssigneeVO av = new TaskAssigneeVO();
            av.setEmployeeId(a.getEmployeeId());
            av.setEmployeeName(nameMap.get(a.getEmployeeId()));
            av.setStatus(a.getStatus());
            av.setProgress(a.getProgress());
            av.setFeedback(a.getFeedback());
            av.setAcceptTime(a.getAcceptTime());
            av.setFinishTime(a.getFinishTime());
            av.setScoreGrade(a.getScoreGrade());
            av.setScoreGradeLabel(a.getScoreGrade() == null
                    ? null
                    : SCORE_GRADE_LABEL.getOrDefault(a.getScoreGrade(), String.valueOf(a.getScoreGrade())));
            av.setScoreBonus(a.getScoreBonus());
            av.setScoredBy(a.getScoredBy());
            av.setScoredByName(nameMap.get(a.getScoredBy()));
            av.setScoredAt(a.getScoredAt());
            return av;
        }).toList());
        vo.setLogs(logs.stream().map(l -> {
            TaskLogVO lv = new TaskLogVO();
            lv.setAction(l.getAction());
            lv.setRemark(l.getRemark());
            lv.setOperatorId(l.getOperatorId());
            lv.setOperatorName(nameMap.get(l.getOperatorId()));
            lv.setCreatedAt(l.getCreatedAt());
            return lv;
        }).toList());
        vo.setAttachments(taskAttachmentService.listAttachmentsUnchecked(taskId));
        return vo;
    }

    /**
     * 驳回任务：仅待接收可驳回。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long taskId, TaskRejectDTO dto) {
        Task task = requireTask(taskId);
        TaskAssignee a = getMyAssignee(taskId);
        if (a.getStatus() == null || a.getStatus() != 0) {
            throw new BusinessException("仅待接收的任务可以驳回");
        }
        a.setStatus(3);
        a.setRejectReason(dto.getReason());
        taskAssigneeMapper.updateById(a);
        saveLog(taskId, a.getEmployeeId(), "REJECT", dto.getReason());

        Employee rejector = employeeMapper.selectById(a.getEmployeeId());
        String rejectorName = rejector != null ? rejector.getName() : "执行人";
        notificationService.sendToEmployees(
                List.of(task.getCreatorId()),
                "任务被驳回：" + task.getTitle(),
                rejectorName + "：" + dto.getReason(),
                NotificationBizType.TASK_REJECT,
                taskId,
                NotificationBizType.DEFAULT_TASK_LINK);
    }

    /**
     * 关闭任务：创建人或 HR/超管；未完成执行人标记为已关闭。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long taskId) {
        Task task = requireTask(taskId);
        Long myId = SecurityUtil.requireEmployeeId();
        boolean canClose = SecurityUtil.isHrStaff() || Objects.equals(task.getCreatorId(), myId);
        if (!canClose) {
            throw new BusinessException("仅创建人或人事管理员可关闭任务");
        }
        if (task.getStatus() != null && task.getStatus() == 3) {
            throw new BusinessException("任务已关闭");
        }
        if (task.getStatus() != null && task.getStatus() == 2) {
            throw new BusinessException("任务已完成，无需关闭");
        }

        task.setStatus(3);
        taskMapper.updateById(task);

        List<TaskAssignee> assignees = taskAssigneeMapper.selectList(
                new LambdaQueryWrapper<TaskAssignee>().eq(TaskAssignee::getTaskId, taskId));
        for (TaskAssignee a : assignees) {
            if (a.getStatus() != null && (a.getStatus() == 0 || a.getStatus() == 1)) {
                a.setStatus(4);
                taskAssigneeMapper.updateById(a);
            }
        }
        saveLog(taskId, myId, "CLOSE", "关闭任务");
        if (task.getParentId() != null && task.getParentId() > 0) {
            refreshParentFromChildren(task.getParentId());
        }
        notifyProjectProgress(task.getProjectId());
    }

    /**
     * 催办：创建人或上级角色，仅写 URGE 日志。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void urge(Long taskId) {
        Task task = requireTask(taskId);
        Long myId = SecurityUtil.requireEmployeeId();
        boolean canUrge = SecurityUtil.isManagerUp() || Objects.equals(task.getCreatorId(), myId);
        if (!canUrge) {
            throw new BusinessException("无权催办该任务");
        }
        if (task.getStatus() != null && (task.getStatus() == 2 || task.getStatus() == 3)) {
            throw new BusinessException("任务已结束，无法催办");
        }
        saveLog(taskId, myId, "URGE", "催办任务");

        List<TaskAssignee> pending = taskAssigneeMapper.selectList(
                new LambdaQueryWrapper<TaskAssignee>()
                        .eq(TaskAssignee::getTaskId, taskId)
                        .in(TaskAssignee::getStatus, List.of(0, 1)));
        List<Long> empIds = pending.stream().map(TaskAssignee::getEmployeeId).toList();
        if (!empIds.isEmpty()) {
            notificationService.sendToEmployees(
                    empIds,
                    "任务催办：" + task.getTitle(),
                    "请尽快处理该任务",
                    NotificationBizType.TASK_URGE,
                    taskId,
                    NotificationBizType.DEFAULT_TASK_LINK);
        }
    }

    /**
     * 我的待办：执行状态为待接收或进行中。
     */
    @Override
    public List<TaskVO> listMyTodo() {
        return listMyTasksByAssigneeStatus(List.of(0, 1), false);
    }

    /**
     * 我的逾期任务。
     */
    @Override
    public List<TaskVO> listMyOverdue() {
        return listMyTasksByAssigneeStatus(List.of(0, 1), true);
    }

    /**
     * 工作台统计：待办数与逾期数。
     */
    @Override
    public TaskTodoStatsVO myTodoStats() {
        List<TaskVO> todo = listMyTodo();
        TaskTodoStatsVO stats = new TaskTodoStatsVO();
        stats.setTodoCount(todo.size());
        stats.setOverdueCount(todo.stream().filter(t -> Boolean.TRUE.equals(t.getOverdue())).count());
        return stats;
    }

    private static final int BOARD_COLUMN_LIMIT = 50;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scoreAssignee(Long taskId, Long employeeId, TaskScoreDTO dto) {
        Task task = requireTask(taskId);
        Long myId = SecurityUtil.requireEmployeeId();
        boolean canScore = SecurityUtil.isHrStaff()
                || SecurityUtil.isManagerUp()
                || Objects.equals(task.getCreatorId(), myId);
        if (!canScore) {
            throw new BusinessException("无权对任务评分");
        }

        TaskAssignee assignee = taskAssigneeMapper.selectOne(
                new LambdaQueryWrapper<TaskAssignee>()
                        .eq(TaskAssignee::getTaskId, taskId)
                        .eq(TaskAssignee::getEmployeeId, employeeId));
        if (assignee == null) {
            throw new BusinessException("该执行人不存在");
        }
        if (assignee.getStatus() == null || assignee.getStatus() != 2) {
            throw new BusinessException("仅可对已完成的执行人评分");
        }

        TaskScoreBonusDict dict = taskScoreBonusDictMapper.selectOne(
                new LambdaQueryWrapper<TaskScoreBonusDict>()
                        .eq(TaskScoreBonusDict::getGrade, dto.getGrade())
                        .eq(TaskScoreBonusDict::getStatus, 1)
                        .last("LIMIT 1"));
        if (dict == null) {
            throw new BusinessException("评分奖金字典未配置该等级，请联系 HR");
        }

        BigDecimal bonus = dict.getBonusAmount() == null ? BigDecimal.ZERO : dict.getBonusAmount();
        assignee.setScoreGrade(dto.getGrade());
        assignee.setScoreBonus(bonus);
        assignee.setScoredBy(myId);
        assignee.setScoredAt(LocalDateTime.now());
        taskAssigneeMapper.updateById(assignee);

        String label = dict.getGradeLabel() != null
                ? dict.getGradeLabel()
                : SCORE_GRADE_LABEL.getOrDefault(dto.getGrade(), String.valueOf(dto.getGrade()));
        saveLog(taskId, myId, "SCORE",
                "对执行人评分：" + label + "，奖金 " + bonus);
    }

    /**
     * 看板：同一权限范围下按主任务状态分列，每列最多 50 条。
     */
    @Override
    public TaskBoardVO board(String scope) {
        TaskBoardVO board = new TaskBoardVO();
        board.setPending(listForBoardColumn(scope, 0));
        board.setInProgress(listForBoardColumn(scope, 1));
        board.setDone(listForBoardColumn(scope, 2));
        board.setClosed(listForBoardColumn(scope, 3));
        return board;
    }

    private List<TaskVO> listForBoardColumn(String scope, int status) {
        PageQuery page = new PageQuery();
        page.setPageNum(1);
        page.setPageSize(BOARD_COLUMN_LIMIT);
        PageResult<TaskVO> pageResult = listTasks(scope, status, page);
        return pageResult.getRecords() == null ? new ArrayList<>() : pageResult.getRecords();
    }

    /**
     * 按当前用户执行状态加载任务列表；onlyOverdue=true 时仅返回逾期。
     */
    private List<TaskVO> listMyTasksByAssigneeStatus(List<Integer> assigneeStatuses, boolean onlyOverdue) {
        Long myId = SecurityUtil.requireEmployeeId();
        List<TaskAssignee> myAssignees = taskAssigneeMapper.selectList(
                new LambdaQueryWrapper<TaskAssignee>()
                        .eq(TaskAssignee::getEmployeeId, myId)
                        .in(TaskAssignee::getStatus, assigneeStatuses));
        if (myAssignees.isEmpty()) {
            return List.of();
        }
        Map<Long, TaskAssignee> myMap = myAssignees.stream()
                .collect(Collectors.toMap(TaskAssignee::getTaskId, Function.identity(), (a, b) -> a));
        List<Task> tasks = taskMapper.selectBatchIds(myMap.keySet());
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        Set<Long> creatorIds = tasks.stream()
                .map(Task::getCreatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> creatorNames = loadEmployeeNames(creatorIds);
        List<Long> taskIds = tasks.stream().map(Task::getId).toList();
        Map<Long, Integer> progressMap = loadAvgProgressByTaskIds(taskIds);
        Set<Long> parentsWithChildren = loadParentIdsHavingChildren(taskIds);
        LocalDateTime now = LocalDateTime.now();
        return tasks.stream()
                .map(task -> toTaskVO(task, creatorNames.get(task.getCreatorId()),
                        myMap.get(task.getId()),
                        progressMap.getOrDefault(task.getId(), 0),
                        parentsWithChildren.contains(task.getId()),
                        now))
                .filter(vo -> !onlyOverdue || Boolean.TRUE.equals(vo.getOverdue()))
                .toList();
    }

    /**
     * 批量计算任务整体进度：有子任务时取子任务进度均值，否则取执行人 progress 均值。
     */
    private Map<Long, Integer> loadAvgProgressByTaskIds(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }
        List<Task> children = taskMapper.selectList(
                new LambdaQueryWrapper<Task>().in(Task::getParentId, taskIds));
        Map<Long, List<Long>> childrenByParent = children.stream()
                .collect(Collectors.groupingBy(Task::getParentId,
                        Collectors.mapping(Task::getId, Collectors.toList())));

        Set<Long> leafOrChildIds = new HashSet<>(taskIds);
        children.forEach(c -> leafOrChildIds.add(c.getId()));

        Map<Long, Integer> assigneeAvg = loadAssigneeAvgProgress(new ArrayList<>(leafOrChildIds));
        Map<Long, Integer> result = new java.util.HashMap<>();
        for (Long taskId : taskIds) {
            List<Long> childIds = childrenByParent.get(taskId);
            if (childIds != null && !childIds.isEmpty()) {
                double avg = childIds.stream()
                        .mapToInt(id -> assigneeAvg.getOrDefault(id, 0))
                        .average()
                        .orElse(0);
                result.put(taskId, (int) Math.round(avg));
            } else {
                result.put(taskId, assigneeAvg.getOrDefault(taskId, 0));
            }
        }
        return result;
    }

    private Map<Long, Integer> loadAssigneeAvgProgress(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }
        List<TaskAssignee> assignees = taskAssigneeMapper.selectList(
                new LambdaQueryWrapper<TaskAssignee>().in(TaskAssignee::getTaskId, taskIds));
        if (assignees.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<TaskAssignee>> grouped = assignees.stream()
                .collect(Collectors.groupingBy(TaskAssignee::getTaskId));
        Map<Long, Integer> result = new java.util.HashMap<>();
        grouped.forEach((taskId, list) -> {
            double avg = list.stream()
                    .mapToInt(a -> a.getProgress() == null ? 0 : a.getProgress())
                    .average()
                    .orElse(0);
            result.put(taskId, (int) Math.round(avg));
        });
        return result;
    }

    private Set<Long> loadParentIdsHavingChildren(List<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Set.of();
        }
        return taskMapper.selectList(
                        new LambdaQueryWrapper<Task>()
                                .select(Task::getId, Task::getParentId)
                                .in(Task::getParentId, parentIds))
                .stream()
                .map(Task::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 根据子任务状态回写父任务：全部完成→已完成；否则存在进行中/待接收则保持进行中。
     */
    private void refreshParentFromChildren(Long parentId) {
        if (parentId == null || parentId <= 0) {
            return;
        }
        Task parent = taskMapper.selectById(parentId);
        if (parent == null || (parent.getStatus() != null && parent.getStatus() == 3)) {
            return;
        }
        List<Task> children = taskMapper.selectList(
                new LambdaQueryWrapper<Task>().eq(Task::getParentId, parentId));
        if (children.isEmpty()) {
            return;
        }
        boolean allDone = children.stream().allMatch(c -> c.getStatus() != null && c.getStatus() == 2);
        boolean anyActive = children.stream().anyMatch(c ->
                c.getStatus() != null && (c.getStatus() == 0 || c.getStatus() == 1));
        Integer old = parent.getStatus();
        if (allDone) {
            parent.setStatus(2);
        } else if (anyActive || (old != null && old == 2)) {
            parent.setStatus(1);
        }
        if (!Objects.equals(old, parent.getStatus())) {
            taskMapper.updateById(parent);
        }
        notifyProjectProgress(parent.getProjectId());
    }

    private void notifyProjectProgress(Long projectId) {
        if (projectId == null) {
            return;
        }
        ProjectService projectService = projectServiceProvider.getIfAvailable();
        if (projectService != null) {
            projectService.refreshProgressFromTasks(projectId);
        }
    }

    /**
     * 根据执行人完成情况回写主任务状态：全员完成→已完成，否则至少为进行中。
     * 若存在子任务，则以子任务完成情况为准。
     */
    private void refreshTaskStatus(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        Long childCount = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getParentId, taskId));
        if (childCount != null && childCount > 0) {
            refreshParentFromChildren(taskId);
            return;
        }
        Long unfinished = taskAssigneeMapper.selectCount(
                new LambdaQueryWrapper<TaskAssignee>()
                        .eq(TaskAssignee::getTaskId, taskId)
                        .ne(TaskAssignee::getStatus, 2));
        if (unfinished != null && unfinished == 0) {
            task.setStatus(2);
            taskMapper.updateById(task);
        } else if (task.getStatus() == null || task.getStatus() == 0) {
            task.setStatus(1);
            taskMapper.updateById(task);
        }
    }

    /** 按 ID 加载任务，不存在则抛业务异常。 */
    private Task requireTask(Long taskId) {
        if (taskId == null) {
            throw new BusinessException("任务ID不能为空");
        }
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("该任务不存在");
        }
        return task;
    }

    /**
     * 查询当前登录员工在该任务下的执行记录；非执行人则无权操作。
     */
    private TaskAssignee getMyAssignee(Long taskId) {
        Long myId = SecurityUtil.requireEmployeeId();
        TaskAssignee a = taskAssigneeMapper.selectOne(new LambdaQueryWrapper<TaskAssignee>()
                .eq(TaskAssignee::getTaskId, taskId)
                .eq(TaskAssignee::getEmployeeId, myId));
        if (a == null) {
            throw new BusinessException("无权操作该任务");
        }
        return a;
    }

    /** 批量查询员工 ID → 姓名映射。 */
    private Map<Long, String> loadEmployeeNames(Set<Long> empIds) {
        if (empIds == null || empIds.isEmpty()) {
            return Map.of();
        }
        return employeeMapper.selectBatchIds(empIds).stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getName, (a, b) -> a));
    }

    /** 截止时间已过且任务仍为待接收/进行中则视为逾期。 */
    private boolean isOverdue(Task task, LocalDateTime now) {
        return task.getDueTime() != null
                && task.getDueTime().isBefore(now)
                && task.getStatus() != null
                && (task.getStatus() == 0 || task.getStatus() == 1);
    }

    /** 列表项 VO 转换。 */
    private TaskVO toTaskVO(Task task, String creatorName, TaskAssignee myAssignee,
                            Integer overallProgress, boolean hasChildren, LocalDateTime now) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setTitle(task.getTitle());
        vo.setContent(task.getContent());
        vo.setParentId(task.getParentId());
        vo.setProjectId(task.getProjectId());
        vo.setHasChildren(hasChildren);
        vo.setPriority(task.getPriority());
        vo.setStatus(task.getStatus());
        vo.setStartTime(task.getStartTime());
        vo.setDueTime(task.getDueTime());
        vo.setCreatorId(task.getCreatorId());
        vo.setCreatorName(creatorName);
        if (myAssignee != null) {
            vo.setMyStatus(myAssignee.getStatus());
            vo.setMyProgress(myAssignee.getProgress());
        }
        vo.setProgress(overallProgress == null ? 0 : overallProgress);
        vo.setOverdue(isOverdue(task, now));
        return vo;
    }

    /**
     * 追加任务操作日志（CREATE/ACCEPT/PROGRESS/FINISH 等），供各业务复用。
     */
    private void saveLog(Long taskId, Long operatorId, String action, String remark) {
        TaskLog log = new TaskLog();
        log.setTaskId(taskId);
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setRemark(remark);
        taskLogMapper.insert(log);
    }
}
