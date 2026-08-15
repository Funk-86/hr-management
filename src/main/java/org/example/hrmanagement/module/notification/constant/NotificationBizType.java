package org.example.hrmanagement.module.notification.constant;

public final class NotificationBizType {

    public static final String TASK_ASSIGN = "TASK_ASSIGN";
    public static final String TASK_URGE = "TASK_URGE";
    public static final String TASK_REJECT = "TASK_REJECT";
    /** 任务逾期定时提醒 */
    public static final String TASK_OVERDUE = "TASK_OVERDUE";
    public static final String PROBATION_REMIND = "PROBATION_REMIND";

    public static final String DEFAULT_TASK_LINK = "/hr/task";
    public static final String DEFAULT_EMPLOYEE_LINK = "/hr/employee";

    private NotificationBizType() {
    }
}
