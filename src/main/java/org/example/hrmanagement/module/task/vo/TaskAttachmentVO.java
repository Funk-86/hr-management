package org.example.hrmanagement.module.task.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskAttachmentVO {

    private Long id;
    private Long taskId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private Long uploaderId;
    private String uploaderName;
    /** 公网访问 URL */
    private String url;
    private LocalDateTime createdAt;
}
