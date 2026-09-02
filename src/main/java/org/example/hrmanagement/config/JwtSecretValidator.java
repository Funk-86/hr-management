package org.example.hrmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * 生产环境强制校验 JWT 密钥，禁止使用内置默认值。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtSecretValidator implements ApplicationRunner {

    private static final Set<String> FORBIDDEN_SECRETS = Set.of(
            "hr-management-jwt-secret-key-2026-dev-only!!",
            "hr-docker-jwt-secret-change-me-32chars!!"
    );

    private static final int MIN_LENGTH = 32;

    private final JwtProperties jwtProperties;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        if (!isProdProfile()) {
            return;
        }
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "生产环境必须设置 JWT_SECRET 环境变量（长度至少 " + MIN_LENGTH + " 字符）");
        }
        if (secret.length() < MIN_LENGTH) {
            throw new IllegalStateException(
                    "生产环境 JWT_SECRET 长度至少 " + MIN_LENGTH + " 字符，当前长度=" + secret.length());
        }
        if (FORBIDDEN_SECRETS.contains(secret)) {
            throw new IllegalStateException(
                    "生产环境 JWT_SECRET 不得使用内置默认值，请在 deploy/.env 中配置强随机密钥");
        }
        log.info("JWT_SECRET 生产校验通过");
    }

    private boolean isProdProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch("prod"::equalsIgnoreCase);
    }
}
