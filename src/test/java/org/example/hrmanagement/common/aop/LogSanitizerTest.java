package org.example.hrmanagement.common.aop;

import org.example.hrmanagement.module.auth.dto.LoginDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogSanitizerTest {

    @Test
    void shouldOmitArgsForLoginUri() {
        LogSanitizer sanitizer = new LogSanitizer(new MockEnvironment());
        assertTrue(sanitizer.shouldOmitArgs("/api/auth/login"));
        assertTrue(sanitizer.shouldOmitArgs("/auth/verify-password"));
        assertFalse(sanitizer.shouldOmitArgs("/employees/1"));
    }

    @Test
    void shouldMaskLoginDtoPassword() {
        LogSanitizer sanitizer = new LogSanitizer(new MockEnvironment());
        LoginDTO dto = new LoginDTO();
        dto.setUsername("alice");
        dto.setPassword("Secret123!");
        dto.setRoleCode("EMPLOYEE");
        dto.setMfaCode("123456");

        String sanitized = sanitizer.sanitizeArgs(new Object[] { dto });
        assertFalse(sanitized.contains("Secret123!"));
        assertFalse(sanitized.contains("123456"));
        assertTrue(sanitized.contains("password=***"));
        assertTrue(sanitized.contains("alice"));
    }

    @Test
    void prodProfileUsesSimpleTypeNameForUnknownArgs() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        LogSanitizer sanitizer = new LogSanitizer(env);
        String sanitized = sanitizer.sanitizeArgs(new Object[] { java.util.Map.of("k", "v") });
        assertTrue(sanitized.contains("Map"));
    }
}
