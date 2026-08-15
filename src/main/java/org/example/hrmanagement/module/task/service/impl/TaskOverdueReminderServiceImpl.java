package org.example.hrmanagement.module.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hrmanagement.module.auth.entity.User;
import org.example.hrmanagement.module.auth.mapper.UserMapper;
import org.example.hrmanagement.module.notification.constant.NotificationBizType;
import org.example.hrmanagement.module.notification.entity.Notification;
import org.example.hrmanagement.module.notification.mapper.NotificationMapper;
import org.example.hrmanagement.module.notification.service.NotificationService;
import org.example.hrmanagement.module.task.entity.Task;
import org.example.hrmanagement.module.task.entity.TaskAssignee;
import org.example.hrmanagement.module.task.mapper.TaskAssigneeMapper;
import org.example.hrmanagement.module.task.mapper.TaskMapper;
import org.example.hrmanagement.module.task.service.TaskOverdueReminderService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskOverdueReminderServiceImpl implements TaskOverdueReminderService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int BATCH_LIMIT = 200;

    private final TaskMapper taskMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;
    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationService notificationService;

    @Override
    public int runReminder() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        List<Task> overdueTasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .in(Task::getStatus, List.of(0, 1))
                        .isNotNull(Task::getDueTime)
                        .lt(Task::getDueTime, now)
                        .orderByAsc(Task::getDueTime)
                        .last("LIMIT " + BATCH_LIMIT));

        if (overdueTasks.isEmpty()) {
            log.info("[任务逾期提醒] 当前无逾期任务");
            return 0;
        }

        int sent = 0;
        for (Task task : overdueTasks) {
            List<TaskAssignee> unfinished = taskAssigneeMapper.selectList(
                    new LambdaQueryWrapper<TaskAssignee>()
                            .eq(TaskAssignee::getTaskId, task.getId())
                            .in(TaskAssignee::getStatus, List.of(0, 1)));

            Set<Long> employeeIds = new HashSet<>();
            for (TaskAssignee a : unfinished) {
                if (a.getEmployeeId() != null) {
                    employeeIds.add(a.getEmployeeId());
                }
            }
            if (task.getCreatorId() != null) {
                employeeIds.add(task.getCreatorId());
            }
            if (employeeIds.isEmpty()) {
                continue;
            }

            String dueText = task.getDueTime() == null ? "未设置" : DT.format(task.getDueTime());
            String title = "任务逾期：" + (StringUtils.hasText(task.getTitle()) ? task.getTitle() : "未命名任务");
            String content = String.format(
                    "任务「%s」已超过截止时间（%s），请尽快处理或催办。",
                    task.getTitle(),
                    dueText);
            String link = NotificationBizType.DEFAULT_TASK_LINK + "?taskId=" + task.getId();

            for (Long empId : employeeIds) {
                Long userId = resolveUserId(empId);
                if (userId == null || alreadyNotifiedToday(userId, task.getId(), dayStart)) {
                    continue;
                }
                notificationService.sendToUsers(
                        List.of(userId),
                        title,
                        content,
                        NotificationBizType.TASK_OVERDUE,
                        task.getId(),
                        link);
                sent++;
            }
        }

        log.info("[任务逾期提醒] 扫描 {} 条任务，新发送 {} 条通知", overdueTasks.size(), sent);
        return sent;
    }

    private Long resolveUserId(Long employeeId) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmployeeId, employeeId)
                        .eq(User::getStatus, 1)
                        .last("LIMIT 1"));
        return user == null ? null : user.getId();
    }

    private boolean alreadyNotifiedToday(Long userId, Long taskId, LocalDateTime dayStart) {
        Long count = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getBizType, NotificationBizType.TASK_OVERDUE)
                        .eq(Notification::getBizId, taskId)
                        .ge(Notification::getCreatedAt, dayStart));
        return count != null && count > 0;
    }
}
