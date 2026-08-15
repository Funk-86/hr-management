package org.example.hrmanagement.module.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogVO {

    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String operation;
    private String method;
    private String params;
    private String ip;
    /** 0-失败 1-成功 */
    private Integer status;
    private String errorMsg;
    private Long duration;
    private LocalDateTime createdAt;
}
