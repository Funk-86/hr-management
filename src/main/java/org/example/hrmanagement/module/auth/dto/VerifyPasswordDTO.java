package org.example.hrmanagement.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyPasswordDTO {

    @NotBlank(message = "密码不能为空")
    private String password;
}
