package org.example.hrmanagement.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 登录身份角色编码，与 sys_role.role_code 一致 */
    @NotBlank(message = "请选择登录角色")
    private String roleCode;

    /** 若账号已开启 MFA，需填写 Authenticator 动态验证码 */
    private String mfaCode;
}
