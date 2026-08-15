package org.example.hrmanagement.module.ai.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.module.ai.config.AiProperties;
import org.example.hrmanagement.module.ai.dto.ChatMessageDTO;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调用 OpenAI 兼容 Chat Completions（Ollama / 通义等）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiClient {

    private final RestClient aiRestClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public String chat(List<ChatMessageDTO> messages) {
        if (!aiProperties.isEnabled()) {
            throw new BusinessException("AI 助手未启用");
        }
        if (messages == null || messages.isEmpty()) {
            throw new BusinessException("消息不能为空");
        }

        List<Map<String, String>> msgList = new ArrayList<>();
        for (ChatMessageDTO m : messages) {
            Map<String, String> item = new HashMap<>();
            item.put("role", m.getRole() == null ? "user" : m.getRole());
            item.put("content", m.getContent() == null ? "" : m.getContent());
            msgList.add(item);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getModel());
        body.put("messages", msgList);
        body.put("stream", false);

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            log.error("序列化 AI 请求失败", e);
            throw new BusinessException("AI 请求构造失败");
        }

        try {
            String raw = aiRestClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);
            if (!StringUtils.hasText(raw)) {
                throw new BusinessException("AI 服务暂不可用：上游返回空响应");
            }
            JsonNode root = objectMapper.readTree(raw);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || !StringUtils.hasText(content.asText())) {
                log.warn("AI 响应无 content: {}", raw);
                throw new BusinessException("AI 服务暂不可用：响应格式异常");
            }
            return content.asText().trim();
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.error("调用 AI 上游失败 status={} body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new BusinessException("AI 服务暂不可用：上游返回 " + e.getStatusCode().value());
        } catch (RestClientException e) {
            log.error("调用 AI 上游失败: {}", e.getMessage());
            throw new BusinessException("AI 服务暂不可用：无法连接 " + aiProperties.getBaseUrl()
                    + "（请确认 Ollama 已启动）");
        } catch (Exception e) {
            log.error("解析 AI 响应失败", e);
            throw new BusinessException("AI 服务暂不可用：" + e.getMessage());
        }
    }
}
