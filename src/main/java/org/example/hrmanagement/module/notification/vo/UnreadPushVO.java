package org.example.hrmanagement.module.notification.vo;

import lombok.Data;

@Data
public class UnreadPushVO {
    private long unreadCount;
    private NotificationVO latest;
}
