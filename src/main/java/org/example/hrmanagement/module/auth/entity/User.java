package org.example.hrmanagement.module.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.BaseEntity;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

    /** 登录用户名 */
    private String username;

    /** 密码（BCrypt加密） */
    private String password;

    /** 关联员工ID */
    private Long employeeId;

    /** 头像 OSS 对象 Key（无关联员工时使用） */
    private String avatar;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 最后登录时间 */
    private LocalDateTime lastLogin;
}
