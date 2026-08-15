package org.example.hrmanagement.module.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AiChatRequest {

    @NotEmpty(message = "消息列表不能为空")
    @Valid
    private List<ChatMessageDTO> messages;
}
