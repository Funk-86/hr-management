package org.example.hrmanagement.module.task;

import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.notification.service.NotificationService;
import org.example.hrmanagement.module.task.entity.Task;
import org.example.hrmanagement.module.task.entity.TaskAssignee;
import org.example.hrmanagement.module.task.mapper.TaskAssigneeMapper;
import org.example.hrmanagement.module.task.mapper.TaskHallDeductMapper;
import org.example.hrmanagement.module.task.mapper.TaskLogMapper;
import org.example.hrmanagement.module.task.mapper.TaskMapper;
import org.example.hrmanagement.module.task.service.impl.TaskHallServiceImpl;
import org.example.hrmanagement.module.task.vo.TaskHallClaimResultVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskHallServiceTest {

    @Mock
    private TaskMapper taskMapper;
    @Mock
    private TaskAssigneeMapper taskAssigneeMapper;
    @Mock
    private TaskHallDeductMapper taskHallDeductMapper;
    @Mock
    private TaskLogMapper taskLogMapper;
    @Mock
    private EmployeeMapper employeeMapper;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TaskHallServiceImpl taskHallService;

    private MockedStatic<SecurityUtil> security;

    @BeforeEach
    void setUp() {
        security = mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        security.close();
    }

    @Test
    void claimSuccessWhenQuotaAvailable() {
        security.when(() -> SecurityUtil.hasRole("EMPLOYEE")).thenReturn(true);
        security.when(SecurityUtil::requireEmployeeId).thenReturn(10L);

        Employee me = new Employee();
        me.setId(10L);
        me.setDeptId(2L);
        me.setName("张三");
        when(employeeMapper.selectById(10L)).thenReturn(me);

        Task task = openTask(100L, 2L, 2, 0);
        when(taskMapper.selectByIdForUpdate(100L)).thenReturn(task);
        when(taskAssigneeMapper.selectCount(any())).thenReturn(0L, 1L);
        when(taskAssigneeMapper.selectOne(any())).thenReturn(null);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);

        TaskHallClaimResultVO vo = taskHallService.claim(100L);

        assertEquals(1, vo.getClaimedCount());
        assertEquals(2, vo.getClaimQuota());
        assertEquals(0, vo.getTaskStatus());
        verify(taskAssigneeMapper).insert(org.mockito.ArgumentMatchers.<TaskAssignee>any());
    }

    @Test
    void claimFailsWhenQuotaFull() {
        security.when(() -> SecurityUtil.hasRole("EMPLOYEE")).thenReturn(true);
        security.when(SecurityUtil::requireEmployeeId).thenReturn(10L);

        Employee me = new Employee();
        me.setId(10L);
        me.setDeptId(2L);
        when(employeeMapper.selectById(10L)).thenReturn(me);

        Task task = openTask(100L, 2L, 1, 0);
        when(taskMapper.selectByIdForUpdate(100L)).thenReturn(task);
        when(taskAssigneeMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> taskHallService.claim(100L));
        assertTrue(ex.getMessage().contains("名额已满"));
        verify(taskAssigneeMapper, never()).insert(org.mockito.ArgumentMatchers.<TaskAssignee>any());
    }

    @Test
    void claimFailsWhenAlreadyClaimed() {
        security.when(() -> SecurityUtil.hasRole("EMPLOYEE")).thenReturn(true);
        security.when(SecurityUtil::requireEmployeeId).thenReturn(10L);

        Employee me = new Employee();
        me.setId(10L);
        me.setDeptId(2L);
        when(employeeMapper.selectById(10L)).thenReturn(me);

        Task task = openTask(100L, 2L, 2, 0);
        when(taskMapper.selectByIdForUpdate(100L)).thenReturn(task);
        when(taskAssigneeMapper.selectCount(any())).thenReturn(1L);

        TaskAssignee existing = new TaskAssignee();
        existing.setEmployeeId(10L);
        existing.setStatus(1);
        when(taskAssigneeMapper.selectOne(any())).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class, () -> taskHallService.claim(100L));
        assertTrue(ex.getMessage().contains("已接取"));
        verify(taskAssigneeMapper, never()).insert(org.mockito.ArgumentMatchers.<TaskAssignee>any());
    }

    @Test
    void claimFillsQuotaSetsInProgress() {
        security.when(() -> SecurityUtil.hasRole("EMPLOYEE")).thenReturn(true);
        security.when(SecurityUtil::requireEmployeeId).thenReturn(10L);

        Employee me = new Employee();
        me.setId(10L);
        me.setDeptId(2L);
        me.setName("李四");
        when(employeeMapper.selectById(10L)).thenReturn(me);

        Task task = openTask(100L, 2L, 1, 0);
        when(taskMapper.selectByIdForUpdate(100L)).thenReturn(task);
        when(taskAssigneeMapper.selectCount(any())).thenReturn(0L, 1L);
        when(taskAssigneeMapper.selectOne(any())).thenReturn(null);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);

        TaskHallClaimResultVO vo = taskHallService.claim(100L);

        assertEquals(1, vo.getClaimedCount());
        assertEquals(1, vo.getClaimQuota());
        assertEquals(1, vo.getTaskStatus());
    }

    private static Task openTask(Long id, Long deptId, int quota, int claimed) {
        Task task = new Task();
        task.setId(id);
        task.setTitle("大厅任务");
        task.setDeptId(deptId);
        task.setClaimMode("OPEN");
        task.setClaimQuota(quota);
        task.setClaimedCount(claimed);
        task.setStatus(0);
        task.setVersion(0);
        task.setCreatorId(1L);
        return task;
    }
}
