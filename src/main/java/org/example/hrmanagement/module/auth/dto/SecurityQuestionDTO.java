package org.example.hrmanagement.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SecurityQuestionDTO {
    @NotBlank(message = "密保问题不能为空")
    @Size(max = 128)
    private String question;

    @NotBlank(message = "密保答案不能为空")
    @Size(min = 1, max = 64)
    private String answer;
}
