package org.example.hrmanagement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /** 允许跨域的来源，生产环境请配置为前端域名 */
    private List<String> allowedOriginPatterns = List.of("*");
}
