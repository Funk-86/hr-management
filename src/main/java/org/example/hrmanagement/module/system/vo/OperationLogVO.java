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
    /** 列表可不返回；详情优先 requestInfo，空则回退 params */
    private String params;
    private String requestInfo;
    private String responseInfo;
    private String ip;
    /** 0-失败 1-成功 */
    private Integer status;
    private String errorMsg;
    private Long duration;
    private LocalDateTime createdAt;
}
