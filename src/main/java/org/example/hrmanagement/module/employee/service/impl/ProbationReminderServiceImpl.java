package org.example.hrmanagement.module.employee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hrmanagement.module.auth.entity.Role;
import org.example.hrmanagement.module.auth.entity.User;
import org.example.hrmanagement.module.auth.entity.UserRole;
import org.example.hrmanagement.module.auth.mapper.RoleMapper;
import org.example.hrmanagement.module.auth.mapper.UserMapper;
import org.example.hrmanagement.module.auth.mapper.UserRoleMapper;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.employee.service.ProbationReminderService;
import org.example.hrmanagement.module.notification.constant.NotificationBizType;
import org.example.hrmanagement.module.notification.entity.Notification;
import org.example.hrmanagement.module.notification.mapper.NotificationMapper;
import org.example.hrmanagement.module.notification.service.NotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProbationReminderServiceImpl implements ProbationReminderService {

    private static final int LOOKAHEAD_DAYS = 7;

    private final EmployeeMapper employeeMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationService notificationService;

    @Override
    public int runReminder() {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(LOOKAHEAD_DAYS);

        // 有试用期结束日，且在未来 7 天内（含今天），未离职
        List<Employee> dueList = employeeMapper.selectList(
                new LambdaQueryWrapper<Employee>()
                        .isNotNull(Employee::getProbationEnd)
                        .ge(Employee::getProbationEnd, today)
                        .le(Employee::getProbationEnd, end)
                        .ne(Employee::getStatus, 3));

        if (dueList.isEmpty()) {
            log.info("[试用期提醒] 未来 {} 天内无即将到期员工", LOOKAHEAD_DAYS);
            return 0;
        }

        List<Long> hrUserIds = listHrStaffUserIds();
        int sent = 0;
        LocalDateTime dayStart = LocalDateTime.of(today, LocalTime.MIN);

        for (Employee emp : dueList) {
            long daysLeft = ChronoUnit.DAYS.between(today, emp.getProbationEnd());
            String hrTitle = "试用期提醒：" + emp.getName();
            String hrContent = String.format(
                    "员工 %s（工号 %s）试用期将于 %s 到期，剩余 %d 天，请及时跟进转正/延期。",
                    emp.getName(),
                    emp.getEmpNo(),
                    emp.getProbationEnd(),
                    daysLeft);

            for (Long userId : hrUserIds) {
                if (alreadyNotifiedToday(userId, emp.getId(), dayStart)) {
                    continue;
                }
                notificationService.sendToUsers(
                        List.of(userId),
                        hrTitle,
                        hrContent,
                        NotificationBizType.PROBATION_REMIND,
                        emp.getId(),
                        NotificationBizType.DEFAULT_EMPLOYEE_LINK);
                sent++;
            }

            User selfUser = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getEmployeeId, emp.getId())
                            .eq(User::getStatus, 1)
                            .last("LIMIT 1"));
            if (selfUser != null && !alreadyNotifiedToday(selfUser.getId(), emp.getId(), dayStart)) {
                notificationService.sendToUsers(
                        List.of(selfUser.getId()),
                        "您的试用期即将到期",
                        String.format("您的试用期将于 %s 到期（剩余 %d 天），请关注转正安排。",
                                emp.getProbationEnd(), daysLeft),
                        NotificationBizType.PROBATION_REMIND,
                        emp.getId(),
                        NotificationBizType.DEFAULT_EMPLOYEE_LINK);
                sent++;
            }
        }

        log.info("[试用期提醒] 扫描 {} 人，新发送消息 {} 条", dueList.size(), sent);
        return sent;
    }

    private List<Long> listHrStaffUserIds() {
        List<Role> roles = roleMapper.selectList(
                new LambdaQueryWrapper<Role>()
                        .in(Role::getRoleCode, List.of("SUPER_ADMIN", "HR_ADMIN"))
                        .eq(Role::getStatus, 1));
        if (roles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = roles.stream().map(Role::getId).toList();
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().in(UserRole::getRoleId, roleIds));
        Set<Long> userIds = new LinkedHashSet<>();
        for (UserRole ur : userRoles) {
            if (ur.getUserId() != null) {
                userIds.add(ur.getUserId());
            }
        }
        return new ArrayList<>(userIds);
    }

    private boolean alreadyNotifiedToday(Long userId, Long employeeId, LocalDateTime dayStart) {
        Long count = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getBizType, NotificationBizType.PROBATION_REMIND)
                        .eq(Notification::getBizId, employeeId)
                        .ge(Notification::getCreatedAt, dayStart));
        return count != null && count > 0;
    }
}
