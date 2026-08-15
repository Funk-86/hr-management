package org.example.hrmanagement.module.auth.vo;

import lombok.Data;

@Data
public class UserSettingVO {

    private boolean notifyAccount;
    private boolean notifySystem;
    private boolean notifyTodo;

    /** 是否已设置登录密码（恒为 true） */
    private boolean passwordSet;
    private String passwordStrength;

    private boolean hasPhone;
    private String maskedPhone;
    private boolean phoneSecured;

    private boolean hasEmail;
    private String maskedEmail;
    private boolean emailSecured;

    private boolean hasSecurityQuestion;
    private String securityQuestion;
    private boolean securityQuestionEnabled;

    private boolean mfaEnabled;
}
