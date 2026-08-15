package org.example.hrmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hrmanagement.module.employee.service.ProbationReminderService;
import org.example.hrmanagement.module.task.service.TaskOverdueReminderService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：试用期提醒、任务逾期提醒、调度心跳。
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTaskConfig {

    private final ProbationReminderService probationReminderService;
    private final TaskOverdueReminderService taskOverdueReminderService;

    /**
     * 试用期到期提醒 — 每天上午 9:00。
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void probationReminder() {
        try {
            int sent = probationReminderService.runReminder();
            log.info("[定时任务] 试用期提醒执行完成，新发送 {} 条", sent);
        } catch (Exception e) {
            log.error("[定时任务] 试用期提醒执行失败", e);
        }
    }

    /**
     * 任务逾期提醒 — 每天上午 9:30。
     * 向未完成执行人及创建人发送站内信（同一用户同一任务每天最多 1 条）。
     */
    @Scheduled(cron = "0 30 9 * * ?")
    public void taskOverdueReminder() {
        try {
            int sent = taskOverdueReminderService.runReminder();
            log.info("[定时任务] 任务逾期提醒执行完成，新发送 {} 条", sent);
        } catch (Exception e) {
            log.error("[定时任务] 任务逾期提醒执行失败", e);
        }
    }

    /**
     * 心跳日志 — 每 30 分钟，确认调度器正常运行。
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void heartbeat() {
        log.debug("[定时任务] 心跳检测 - 调度器正常运行");
    }
}
