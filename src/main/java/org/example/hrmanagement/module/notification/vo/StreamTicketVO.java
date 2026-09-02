package org.example.hrmanagement.module.notification.vo;

import lombok.Data;

@Data
public class StreamTicketVO {
    private String ticket;
    /** 有效秒数 */
    private int expiresIn;
}
