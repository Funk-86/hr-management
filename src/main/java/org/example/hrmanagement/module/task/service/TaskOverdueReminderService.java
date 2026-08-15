package org.example.hrmanagement.module.task.service;

/**
 * 任务逾期站内提醒（定时 / 手动补跑）。
 */
public interface TaskOverdueReminderService {

    /**
     * 扫描已逾期且未完成的任务，向未完成执行人及创建人发送提醒（同一用户同一任务每天最多 1 条）。
     *
     * @return 新发送的通知条数
     */
    int runReminder();
}
