package org.example.hrmanagement.module.auth.vo;

import lombok.Data;

@Data
public class ProfileUpdateVO {
    /** 用户名变更时返回新 Token，前端需替换本地 Token */
    private String token;
    private String username;
}
