package org.example.hrmanagement.module.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_setting")
public class UserSetting extends BaseEntity {

    private Long userId;
    private Integer notifyAccount;
    private Integer notifySystem;
    private Integer notifyTodo;
    private Integer phoneSecured;
    private Integer emailSecured;
    private String securityQuestion;
    private String securityAnswerHash;
    private Integer mfaEnabled;
    private String mfaSecret;
}
