package org.example.hrmanagement.module.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationSettingDTO {
    @NotNull
    private Boolean notifyAccount;
    @NotNull
    private Boolean notifySystem;
    @NotNull
    private Boolean notifyTodo;
}
