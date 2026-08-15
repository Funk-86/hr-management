package org.example.hrmanagement.module.project.service.impl;

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
import org.example.hrmanagement.module.project.dto.ProjectCreateDTO;
import org.example.hrmanagement.module.project.dto.ProjectProgressDTO;
import org.example.hrmanagement.module.project.dto.ProjectUpdateDTO;
import org.example.hrmanagement.module.project.entity.Project;
import org.example.hrmanagement.module.project.entity.ProjectMember;
import org.example.hrmanagement.module.project.mapper.ProjectMapper;
import org.example.hrmanagement.module.project.mapper.ProjectMemberMapper;
import org.example.hrmanagement.module.project.service.ProjectService;
import org.example.hrmanagement.module.project.vo.ProjectVO;
import org.example.hrmanagement.module.task.entity.Task;
import org.example.hrmanagement.module.task.entity.TaskAssignee;
import org.example.hrmanagement.module.task.mapper.TaskAssigneeMapper;
import org.example.hrmanagement.module.task.mapper.TaskMapper;
import org.example.hrmanagement.module.task.vo.TaskVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final TaskMapper taskMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;

    @Override
    public PageResult<ProjectVO> page(String scope, Integer status, PageQuery pageQuery) {
        Long myId = SecurityUtil.requireEmployeeId();
        String resolved = StringUtils.hasText(scope) ? scope : "mine";

        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if ("all".equalsIgnoreCase(resolved) && SecurityUtil.isHrStaff()) {
            // HR 看全部
        } else {
            Set<Long> projectIds = loadRelatedProjectIds(myId);
            if (projectIds.isEmpty()) {
                return PageResult.empty();
            }
            wrapper.in(Project::getId, projectIds);
        }
        if (status != null) {
            wrapper.eq(Project::getStatus, status);
        }
        wrapper.orderByDesc(Project::getCreatedAt);

        IPage<Project> iPage = projectMapper.selectPage(
                new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()), wrapper);
        List<Project> records = iPage.getRecords();
        if (records == null || records.isEmpty()) {
            return PageResult.empty();
        }

        List<ProjectVO> vos = toVoList(records);
        PageResult<ProjectVO> result = new PageResult<>();
        result.setRecords(vos);
        result.setTotal(iPage.getTotal());
        result.setPageNum(iPage.getCurrent());
        result.setPageSize(iPage.getSize());
        result.setPages(iPage.getPages());
        return result;
    }

    @Override
    public ProjectVO getDetail(Long id) {
        Project project = requireProject(id);
        assertCanView(project);
        return toVoList(List.of(project)).get(0);
    }

    @Override
    public List<TaskVO> listProjectTasks(Long projectId) {
        Project project = requireProject(projectId);
        assertCanView(project);
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getProjectId, projectId)
                        .eq(Task::getParentId, 0)
                        .orderByDesc(Task::getCreatedAt));
        if (tasks.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> progressMap = loadRootTaskProgress(tasks);
        Set<Long> creatorIds = tasks.stream()
                .map(Task::getCreatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> names = loadEmployeeNames(creatorIds);
        LocalDateTime now = LocalDateTime.now();
        return tasks.stream().map(t -> {
            TaskVO vo = new TaskVO();
            vo.setId(t.getId());
            vo.setTitle(t.getTitle());
            vo.setContent(t.getContent());
            vo.setParentId(t.getParentId());
            vo.setProjectId(t.getProjectId());
            vo.setPriority(t.getPriority());
            vo.setStatus(t.getStatus());
            vo.setStartTime(t.getStartTime());
            vo.setDueTime(t.getDueTime());
            vo.setCreatorId(t.getCreatorId());
            vo.setCreatorName(names.get(t.getCreatorId()));
            vo.setProgress(progressMap.getOrDefault(t.getId(), 0));
            vo.setOverdue(t.getDueTime() != null
                    && t.getStatus() != null
                    && t.getStatus() < 2
                    && t.getDueTime().isBefore(now));
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProjectCreateDTO dto) {
        if (!SecurityUtil.isManagerUp()) {
            throw new BusinessException("仅经理及以上可创建项目");
        }
        Long myId = SecurityUtil.requireEmployeeId();
        Employee me = employeeMapper.selectById(myId);
        Long ownerId = dto.getOwnerId() == null ? myId : dto.getOwnerId();
        Employee owner = employeeMapper.selectById(ownerId);
        if (owner == null || owner.getStatus() == null || owner.getStatus() == 3) {
            throw new BusinessException("负责人不存在或已离职");
        }

        Project project = new Project();
        project.setName(dto.getName().trim());
        project.setDescription(dto.getDescription());
        project.setOwnerId(ownerId);
        project.setDeptId(dto.getDeptId() != null
                ? dto.getDeptId()
                : (me != null ? me.getDeptId() : owner.getDeptId()));
        project.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        project.setProgress(0);
        project.setProgressLocked(0);
        project.setStartDate(dto.getStartDate());
        project.setEndDate(dto.getEndDate());
        projectMapper.insert(project);

        Set<Long> memberIds = new LinkedHashSet<>();
        memberIds.add(ownerId);
        if (dto.getMemberIds() != null) {
            memberIds.addAll(dto.getMemberIds());
        }
        replaceMembers(project.getId(), memberIds);
        return project.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProjectUpdateDTO dto) {
        Project project = requireProject(id);
        assertCanManage(project);
        if (project.getStatus() != null && project.getStatus() == 3) {
            throw new BusinessException("项目已关闭，无法编辑");
        }

        Long ownerId = dto.getOwnerId() == null ? project.getOwnerId() : dto.getOwnerId();
        Employee owner = employeeMapper.selectById(ownerId);
        if (owner == null || owner.getStatus() == null || owner.getStatus() == 3) {
            throw new BusinessException("负责人不存在或已离职");
        }

        project.setName(dto.getName().trim());
        project.setDescription(dto.getDescription());
        project.setOwnerId(ownerId);
        if (dto.getDeptId() != null) {
            project.setDeptId(dto.getDeptId());
        }
        if (dto.getStatus() != null) {
            project.setStatus(dto.getStatus());
        }
        project.setStartDate(dto.getStartDate());
        project.setEndDate(dto.getEndDate());
        projectMapper.updateById(project);

        if (dto.getMemberIds() != null) {
            Set<Long> memberIds = new LinkedHashSet<>(dto.getMemberIds());
            memberIds.add(ownerId);
            replaceMembers(id, memberIds);
        } else {
            ensureMember(id, ownerId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProgress(Long id, ProjectProgressDTO dto) {
        Project project = requireProject(id);
        assertCanManage(project);
        if (project.getStatus() != null && project.getStatus() == 3) {
            throw new BusinessException("项目已关闭");
        }
        boolean lock = dto.getLocked() == null || Boolean.TRUE.equals(dto.getLocked());
        project.setProgress(dto.getProgress());
        project.setProgressLocked(lock ? 1 : 0);
        if (dto.getProgress() != null && dto.getProgress() >= 100) {
            project.setStatus(2);
        } else if (project.getStatus() != null && project.getStatus() == 0) {
            project.setStatus(1);
        }
        projectMapper.updateById(project);
        if (!lock) {
            refreshProgressFromTasks(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id) {
        Project project = requireProject(id);
        assertCanManage(project);
        if (project.getStatus() != null && project.getStatus() == 3) {
            throw new BusinessException("项目已关闭");
        }
        project.setStatus(3);
        projectMapper.updateById(project);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshProgressFromTasks(Long projectId) {
        if (projectId == null) {
            return;
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return;
        }
        if (project.getProgressLocked() != null && project.getProgressLocked() == 1) {
            return;
        }
        if (project.getStatus() != null && project.getStatus() == 3) {
            return;
        }
        List<Task> roots = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getProjectId, projectId)
                        .eq(Task::getParentId, 0));
        if (roots.isEmpty()) {
            return;
        }
        Map<Long, Integer> progressMap = loadRootTaskProgress(roots);
        int avg = (int) Math.round(roots.stream()
                .mapToInt(t -> progressMap.getOrDefault(t.getId(), 0))
                .average()
                .orElse(0));
        project.setProgress(avg);
        boolean allDone = roots.stream().allMatch(t -> t.getStatus() != null && t.getStatus() == 2);
        if (allDone) {
            project.setStatus(2);
        } else if (project.getStatus() == null || project.getStatus() == 0) {
            project.setStatus(1);
        } else if (project.getStatus() == 2 && !allDone) {
            project.setStatus(1);
        }
        projectMapper.updateById(project);
    }

    @Override
    public long countMyActiveProjects() {
        Long myId = SecurityUtil.getEmployeeId();
        if (myId == null) {
            return 0L;
        }
        Set<Long> ids = loadRelatedProjectIds(myId);
        if (ids.isEmpty()) {
            return 0L;
        }
        Long count = projectMapper.selectCount(
                new LambdaQueryWrapper<Project>()
                        .in(Project::getId, ids)
                        .in(Project::getStatus, 0, 1));
        return count == null ? 0L : count;
    }

    private Set<Long> loadRelatedProjectIds(Long employeeId) {
        Set<Long> ids = new HashSet<>();
        List<Project> owned = projectMapper.selectList(
                new LambdaQueryWrapper<Project>().eq(Project::getOwnerId, employeeId));
        owned.forEach(p -> ids.add(p.getId()));
        List<ProjectMember> members = projectMemberMapper.selectList(
                new LambdaQueryWrapper<ProjectMember>().eq(ProjectMember::getEmployeeId, employeeId));
        members.forEach(m -> ids.add(m.getProjectId()));
        return ids;
    }

    private void replaceMembers(Long projectId, Set<Long> employeeIds) {
        List<ProjectMember> existing = projectMemberMapper.selectList(
                new LambdaQueryWrapper<ProjectMember>().eq(ProjectMember::getProjectId, projectId));
        for (ProjectMember m : existing) {
            projectMemberMapper.deleteById(m.getId());
        }
        for (Long empId : employeeIds) {
            if (empId == null) {
                continue;
            }
            Employee emp = employeeMapper.selectById(empId);
            if (emp == null || emp.getStatus() == null || emp.getStatus() == 3) {
                continue;
            }
            ProjectMember m = new ProjectMember();
            m.setProjectId(projectId);
            m.setEmployeeId(empId);
            projectMemberMapper.insert(m);
        }
    }

    private void ensureMember(Long projectId, Long employeeId) {
        Long cnt = projectMemberMapper.selectCount(
                new LambdaQueryWrapper<ProjectMember>()
                        .eq(ProjectMember::getProjectId, projectId)
                        .eq(ProjectMember::getEmployeeId, employeeId));
        if (cnt != null && cnt > 0) {
            return;
        }
        ProjectMember m = new ProjectMember();
        m.setProjectId(projectId);
        m.setEmployeeId(employeeId);
        projectMemberMapper.insert(m);
    }

    private Project requireProject(Long id) {
        if (id == null) {
            throw new BusinessException("项目ID不能为空");
        }
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        return project;
    }

    private void assertCanView(Project project) {
        if (SecurityUtil.isHrStaff()) {
            return;
        }
        Long myId = SecurityUtil.requireEmployeeId();
        if (Objects.equals(project.getOwnerId(), myId)) {
            return;
        }
        Long cnt = projectMemberMapper.selectCount(
                new LambdaQueryWrapper<ProjectMember>()
                        .eq(ProjectMember::getProjectId, project.getId())
                        .eq(ProjectMember::getEmployeeId, myId));
        if (cnt == null || cnt == 0) {
            throw new BusinessException("无权查看该项目");
        }
    }

    private void assertCanManage(Project project) {
        if (SecurityUtil.isHrStaff()) {
            return;
        }
        Long myId = SecurityUtil.requireEmployeeId();
        if (!Objects.equals(project.getOwnerId(), myId) && !SecurityUtil.isManagerUp()) {
            throw new BusinessException("仅负责人或人事可管理项目");
        }
        if (!Objects.equals(project.getOwnerId(), myId) && SecurityUtil.isManagerUp()) {
            // 经理可管理本部门项目
            Employee me = employeeMapper.selectById(myId);
            if (me == null || me.getDeptId() == null
                    || !Objects.equals(me.getDeptId(), project.getDeptId())) {
                throw new BusinessException("仅可管理本部门项目或本人负责的项目");
            }
        }
    }

    private List<ProjectVO> toVoList(List<Project> projects) {
        Set<Long> ownerIds = projects.stream().map(Project::getOwnerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> deptIds = projects.stream().map(Project::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<Long> projectIds = projects.stream().map(Project::getId).toList();

        List<ProjectMember> members = projectMemberMapper.selectList(
                new LambdaQueryWrapper<ProjectMember>().in(ProjectMember::getProjectId, projectIds));
        Set<Long> memberEmpIds = members.stream().map(ProjectMember::getEmployeeId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> allEmpIds = new HashSet<>(ownerIds);
        allEmpIds.addAll(memberEmpIds);
        Map<Long, String> nameMap = loadEmployeeNames(allEmpIds);

        Map<Long, String> deptNameMap = Map.of();
        if (!deptIds.isEmpty()) {
            deptNameMap = departmentMapper.selectBatchIds(deptIds).stream()
                    .collect(Collectors.toMap(Department::getId, Department::getDeptName, (a, b) -> a));
        }

        Map<Long, List<ProjectMember>> membersByProject = members.stream()
                .collect(Collectors.groupingBy(ProjectMember::getProjectId));

        Map<Long, Long> taskCountMap = new HashMap<>();
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .select(Task::getId, Task::getProjectId)
                        .in(Task::getProjectId, projectIds)
                        .eq(Task::getParentId, 0));
        for (Task t : tasks) {
            taskCountMap.merge(t.getProjectId(), 1L, Long::sum);
        }

        Map<Long, String> finalDeptNameMap = deptNameMap;
        return projects.stream().map(p -> {
            ProjectVO vo = new ProjectVO();
            vo.setId(p.getId());
            vo.setName(p.getName());
            vo.setDescription(p.getDescription());
            vo.setOwnerId(p.getOwnerId());
            vo.setOwnerName(nameMap.get(p.getOwnerId()));
            vo.setDeptId(p.getDeptId());
            vo.setDeptName(finalDeptNameMap.get(p.getDeptId()));
            vo.setStatus(p.getStatus());
            vo.setProgress(p.getProgress() == null ? 0 : p.getProgress());
            vo.setProgressLocked(p.getProgressLocked() != null && p.getProgressLocked() == 1);
            vo.setStartDate(p.getStartDate());
            vo.setEndDate(p.getEndDate());
            vo.setCreatedAt(p.getCreatedAt());
            vo.setTaskCount(taskCountMap.getOrDefault(p.getId(), 0L).intValue());
            List<ProjectMember> ms = membersByProject.getOrDefault(p.getId(), List.of());
            vo.setMemberIds(ms.stream().map(ProjectMember::getEmployeeId).toList());
            vo.setMemberNames(ms.stream()
                    .map(m -> nameMap.getOrDefault(m.getEmployeeId(), String.valueOf(m.getEmployeeId())))
                    .toList());
            return vo;
        }).toList();
    }

    /** 根任务进度：有子任务取子任务执行人均值的再平均，否则取自身执行人均值 */
    private Map<Long, Integer> loadRootTaskProgress(List<Task> roots) {
        if (roots == null || roots.isEmpty()) {
            return Map.of();
        }
        List<Long> rootIds = roots.stream().map(Task::getId).toList();
        List<Task> children = taskMapper.selectList(
                new LambdaQueryWrapper<Task>().in(Task::getParentId, rootIds));
        Map<Long, List<Long>> childrenByParent = children.stream()
                .collect(Collectors.groupingBy(Task::getParentId,
                        Collectors.mapping(Task::getId, Collectors.toList())));

        Set<Long> leafIds = new HashSet<>(rootIds);
        children.forEach(c -> leafIds.add(c.getId()));
        Map<Long, Integer> assigneeAvg = loadAssigneeAvg(new ArrayList<>(leafIds));

        Map<Long, Integer> result = new HashMap<>();
        for (Long rootId : rootIds) {
            List<Long> childIds = childrenByParent.get(rootId);
            if (childIds != null && !childIds.isEmpty()) {
                result.put(rootId, (int) Math.round(childIds.stream()
                        .mapToInt(id -> assigneeAvg.getOrDefault(id, 0))
                        .average()
                        .orElse(0)));
            } else {
                result.put(rootId, assigneeAvg.getOrDefault(rootId, 0));
            }
        }
        return result;
    }

    private Map<Long, Integer> loadAssigneeAvg(List<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        List<TaskAssignee> assignees = taskAssigneeMapper.selectList(
                new LambdaQueryWrapper<TaskAssignee>().in(TaskAssignee::getTaskId, taskIds));
        Map<Long, List<TaskAssignee>> grouped = assignees.stream()
                .collect(Collectors.groupingBy(TaskAssignee::getTaskId));
        Map<Long, Integer> result = new HashMap<>();
        grouped.forEach((tid, list) -> {
            double avg = list.stream()
                    .mapToInt(a -> a.getProgress() == null ? 0 : a.getProgress())
                    .average()
                    .orElse(0);
            result.put(tid, (int) Math.round(avg));
        });
        return result;
    }

    private Map<Long, String> loadEmployeeNames(Set<Long> empIds) {
        if (empIds == null || empIds.isEmpty()) {
            return Map.of();
        }
        return employeeMapper.selectBatchIds(empIds).stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getName, (a, b) -> a));
    }
}
