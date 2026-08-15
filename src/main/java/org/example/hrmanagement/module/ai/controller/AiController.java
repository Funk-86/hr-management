package org.example.hrmanagement.module.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.ai.dto.AiChatRequest;
import org.example.hrmanagement.module.ai.dto.AiTaskDraftRequest;
import org.example.hrmanagement.module.ai.service.AiService;
import org.example.hrmanagement.module.ai.vo.AiChatVO;
import org.example.hrmanagement.module.ai.vo.AiTaskDraftVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 助手")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Operation(summary = "多轮对话")
    @PostMapping("/chat")
    public Result<AiChatVO> chat(@Valid @RequestBody AiChatRequest request) {
        return Result.success(aiService.chat(request));
    }

    @Operation(summary = "生成任务标题与说明草稿")
    @PostMapping("/task-draft")
    public Result<AiTaskDraftVO> taskDraft(@Valid @RequestBody AiTaskDraftRequest request) {
        return Result.success(aiService.taskDraft(request));
    }
}
