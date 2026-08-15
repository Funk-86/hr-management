package org.example.hrmanagement.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaConfirmDTO {
    @NotBlank(message = "验证码不能为空")
    private String code;
}
