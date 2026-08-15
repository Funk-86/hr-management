package org.example.hrmanagement.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** 是否启用 AI 能力 */
    private boolean enabled = true;

    /** OpenAI 兼容接口根路径，如 http://127.0.0.1:11434/v1 */
    private String baseUrl = "http://127.0.0.1:11434/v1";

    /** API Key；Ollama 可填任意非空值 */
    private String apiKey = "ollama";

    /** 模型名 */
    private String model = "qwen2.5:3b";

    /** 调用超时（秒） */
    private int timeoutSeconds = 120;
}
