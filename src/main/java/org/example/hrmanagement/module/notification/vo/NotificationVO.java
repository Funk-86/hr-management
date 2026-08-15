package org.example.hrmanagement.module.notification.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {
    private Long id;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private String link;
    private Integer isRead;
    private LocalDateTime createdAt;
}
