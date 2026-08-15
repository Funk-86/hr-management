package org.example.hrmanagement.module.ai.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiChatVO {
    private String content;
    /** 结构化任务卡片（前端渲染，可点击进详情） */
    private List<AiTaskCardVO> tasks = new ArrayList<>();
}
