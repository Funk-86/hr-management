package org.example.hrmanagement.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SecurityToggleDTO {
    /**
     * phoneSecured | emailSecured | securityQuestion | mfa
     */
    @NotBlank
    private String field;
    @NotNull
    private Boolean enabled;
}
