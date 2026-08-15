package org.example.hrmanagement.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisLoginDTO {
    @NotBlank(message = "用户名不能为空")
    private String oldPassword;

    @NotBlank(message = "密码不能为空")
    private String newPassword;

    @NotBlank(message = "再次输入密码不能为空")
    private String confirmPassword;
}
