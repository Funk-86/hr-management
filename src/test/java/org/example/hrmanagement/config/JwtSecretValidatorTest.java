package org.example.hrmanagement.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtSecretValidatorTest {

    @Test
    void prodRejectsDefaultSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        JwtProperties props = new JwtProperties();
        props.setSecret("hr-management-jwt-secret-key-2026-dev-only!!");
        JwtSecretValidator validator = new JwtSecretValidator(props, env);
        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    @Test
    void prodAcceptsStrongSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        JwtProperties props = new JwtProperties();
        props.setSecret("this-is-a-very-strong-random-production-secret-key!!");
        JwtSecretValidator validator = new JwtSecretValidator(props, env);
        assertDoesNotThrow(() -> validator.run(null));
    }

    @Test
    void devProfileSkipsValidation() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        JwtProperties props = new JwtProperties();
        props.setSecret("hr-management-jwt-secret-key-2026-dev-only!!");
        JwtSecretValidator validator = new JwtSecretValidator(props, env);
        assertDoesNotThrow(() -> validator.run(null));
    }
}
