package org.example.hrmanagement.module.ai.service;

import org.example.hrmanagement.module.ai.dto.AiChatRequest;
import org.example.hrmanagement.module.ai.dto.AiTaskDraftRequest;
import org.example.hrmanagement.module.ai.vo.AiChatVO;
import org.example.hrmanagement.module.ai.vo.AiTaskDraftVO;

public interface AiService {

    AiChatVO chat(AiChatRequest request);

    AiTaskDraftVO taskDraft(AiTaskDraftRequest request);
}
