package org.example.hrmanagement.module.ai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.ai.client.AiClient;
import org.example.hrmanagement.module.ai.dto.AiChatRequest;
import org.example.hrmanagement.module.ai.dto.AiTaskDraftRequest;
import org.example.hrmanagement.module.ai.dto.ChatMessageDTO;
import org.example.hrmanagement.module.ai.service.AiService;
import org.example.hrmanagement.module.ai.vo.AiChatVO;
import org.example.hrmanagement.module.ai.vo.AiTaskCardVO;
import org.example.hrmanagement.module.ai.vo.AiTaskDraftVO;
import org.example.hrmanagement.module.task.service.TaskService;
import org.example.hrmanagement.module.task.vo.TaskVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Map<Integer, String> MY_STATUS = Map.of(
            0, "待接收",
            1, "进行中",
            2, "已完成",
            3, "已驳回",
            4, "已关闭"
    );

    private static final Map<Integer, String> PRIORITY = Map.of(
            1, "低",
            2, "中",
            3, "高"
    );

    private static final Pattern TODO_QUERY = Pattern.compile(
            "待办|未完成|未做完|我的任务|还有哪些任务|逾期任务|任务列表|展示.*任务|查看.*任务");

    private static final String SYSTEM_PROMPT = """
            你是「智汇人事管理系统」的智能助手，只回答与本系统相关的人事、考勤、请假、薪资、任务管理问题。
            系统主要菜单：工作台、部门管理、岗位管理、员工管理、考勤管理、请假管理、薪资管理、任务管理、个人中心。
            任务流程：上级创建并下发 → 执行人接收 → 更新进度（100% 自动完成）→ 可驳回/催办/关闭。
            下方会提供「当前用户实时待办任务」数据。当用户询问未完成/待办/逾期任务时：
            1）用 1-3 句中文做简要总结（数量、是否有逾期），不要逐条罗列明细（界面会用卡片展示）；
            2）不要编造列表外的任务；列表为空则明确说暂无待办。
            回答简洁实用。
            """;

    private static final String TASK_DRAFT_PROMPT = """
            你是人事任务撰写助手。根据用户给出的关键词，生成一条任务草稿。
            严格只输出一段 JSON（不要 Markdown 代码块，不要其它说明），格式：
            {"title":"不超过40字的任务标题","content":"100-200字的任务说明，含目标与要求"}
            """;

    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final TaskService taskService;

    @Override
    public AiChatVO chat(AiChatRequest request) {
        List<ChatMessageDTO> messages = new ArrayList<>();
        List<TaskVO> todos = loadMyTodosSafe();
        messages.add(systemMessage(SYSTEM_PROMPT + "\n\n" + formatTodoContext(todos)));

        String lastUserText = null;
        for (ChatMessageDTO m : request.getMessages()) {
            if (m == null || !StringUtils.hasText(m.getContent())) {
                continue;
            }
            String role = m.getRole() == null ? "user" : m.getRole().trim().toLowerCase();
            if (!"user".equals(role) && !"assistant".equals(role)) {
                role = "user";
            }
            ChatMessageDTO copy = new ChatMessageDTO();
            copy.setRole(role);
            copy.setContent(m.getContent().trim());
            messages.add(copy);
            if ("user".equals(role)) {
                lastUserText = copy.getContent();
            }
        }
        if (messages.size() <= 1) {
            throw new BusinessException("请输入问题");
        }

        AiChatVO vo = new AiChatVO();
        vo.setContent(aiClient.chat(messages));
        if (lastUserText != null && isTodoQuery(lastUserText)) {
            vo.setTasks(toCards(todos));
            if (!StringUtils.hasText(vo.getContent())) {
                vo.setContent(todos.isEmpty()
                        ? "你当前没有待办任务。"
                        : "以下是你的未完成任务，点击卡片可查看详情。");
            }
        }
        return vo;
    }

    @Override
    public AiTaskDraftVO taskDraft(AiTaskDraftRequest request) {
        List<ChatMessageDTO> messages = List.of(
                systemMessage(TASK_DRAFT_PROMPT),
                userMessage("请根据以下需求生成任务草稿：" + request.getPrompt().trim())
        );
        String raw = aiClient.chat(messages);
        return parseTaskDraft(raw, request.getPrompt().trim());
    }

    private boolean isTodoQuery(String text) {
        return TODO_QUERY.matcher(text.toLowerCase(Locale.ROOT)).find();
    }

    private List<TaskVO> loadMyTodosSafe() {
        if (SecurityUtil.getEmployeeId() == null) {
            return List.of();
        }
        try {
            List<TaskVO> todos = taskService.listMyTodo();
            return todos == null ? List.of() : todos;
        } catch (Exception e) {
            log.warn("加载待办失败: {}", e.getMessage());
            return List.of();
        }
    }

    private String formatTodoContext(List<TaskVO> todos) {
        if (SecurityUtil.getEmployeeId() == null) {
            return "【当前用户实时待办任务】\n当前账号未关联员工档案，无法查询个人待办任务。";
        }
        if (todos.isEmpty()) {
            return "【当前用户实时待办任务】\n暂无待办任务（待接收/进行中均为空）。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【当前用户实时待办任务】共 ").append(todos.size()).append(" 条：\n");
        int i = 1;
        for (TaskVO t : todos) {
            sb.append(i++).append(". ")
                    .append(t.getTitle() == null ? "未命名任务" : t.getTitle())
                    .append(" | 状态：").append(MY_STATUS.getOrDefault(t.getMyStatus(), "未知"))
                    .append(" | 进度：").append(t.getMyProgress() == null ? 0 : t.getMyProgress()).append('%')
                    .append(" | 优先级：").append(PRIORITY.getOrDefault(t.getPriority(), "中"))
                    .append(" | 截止：")
                    .append(t.getDueTime() == null ? "未设置" : DT.format(t.getDueTime()))
                    .append(" | 逾期：").append(Boolean.TRUE.equals(t.getOverdue()) ? "是" : "否")
                    .append(" | 创建人：").append(t.getCreatorName() == null ? "-" : t.getCreatorName())
                    .append('\n');
        }
        return sb.toString();
    }

    private List<AiTaskCardVO> toCards(List<TaskVO> todos) {
        List<AiTaskCardVO> cards = new ArrayList<>();
        for (TaskVO t : todos) {
            AiTaskCardVO c = new AiTaskCardVO();
            c.setId(t.getId());
            c.setTitle(t.getTitle());
            c.setMyStatusLabel(MY_STATUS.getOrDefault(t.getMyStatus(), "未知"));
            c.setMyProgress(t.getMyProgress() == null ? 0 : t.getMyProgress());
            c.setPriorityLabel(PRIORITY.getOrDefault(t.getPriority(), "中"));
            c.setDueTime(t.getDueTime() == null ? null : DT.format(t.getDueTime()));
            c.setOverdue(Boolean.TRUE.equals(t.getOverdue()));
            c.setCreatorName(t.getCreatorName());
            cards.add(c);
        }
        return cards;
    }

    private AiTaskDraftVO parseTaskDraft(String raw, String fallbackPrompt) {
        String json = extractJson(raw);
        try {
            JsonNode node = objectMapper.readTree(json);
            AiTaskDraftVO vo = new AiTaskDraftVO();
            vo.setTitle(textOrFallback(node, "title", fallbackPrompt));
            vo.setContent(textOrFallback(node, "content", "请根据「" + fallbackPrompt + "」完成相关工作。"));
            if (vo.getTitle().length() > 40) {
                vo.setTitle(vo.getTitle().substring(0, 40));
            }
            return vo;
        } catch (Exception e) {
            AiTaskDraftVO vo = new AiTaskDraftVO();
            vo.setTitle(fallbackPrompt.length() > 40 ? fallbackPrompt.substring(0, 40) : fallbackPrompt);
            vo.setContent(StringUtils.hasText(raw) ? raw.trim() : "请完成：" + fallbackPrompt);
            return vo;
        }
    }

    private String extractJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "{}";
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return text.substring(start, end + 1);
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String textOrFallback(JsonNode node, String field, String fallback) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || !StringUtils.hasText(v.asText())) {
            return fallback;
        }
        return v.asText().trim();
    }

    private ChatMessageDTO systemMessage(String content) {
        ChatMessageDTO m = new ChatMessageDTO();
        m.setRole("system");
        m.setContent(content);
        return m;
    }

    private ChatMessageDTO userMessage(String content) {
        ChatMessageDTO m = new ChatMessageDTO();
        m.setRole("user");
        m.setContent(content);
        return m;
    }
}
