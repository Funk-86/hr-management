package org.example.hrmanagement.module.employee.service;

/**
 * 试用期到期提醒：扫描即将到期员工并发送站内信。
 */
public interface ProbationReminderService {

    /**
     * 执行一次提醒扫描。
     *
     * @return 本次新发送的消息条数（按接收人计）
     */
    int runReminder();
}
