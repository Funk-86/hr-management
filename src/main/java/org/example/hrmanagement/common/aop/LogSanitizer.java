package org.example.hrmanagement.common.aop;

import org.example.hrmanagement.module.auth.dto.LoginDTO;
import org.example.hrmanagement.module.auth.dto.RegisLoginDTO;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 控制器入参日志脱敏。
 */
@Component
public class LogSanitizer {

    private static final Set<String> NO_ARGS_URI_SUFFIXES = Set.of(
            "/auth/login",
            "/auth/verify-password",
            "/auth/regis-login"
    );

    private final Environment environment;

    public LogSanitizer(Environment environment) {
        this.environment = environment;
    }

    public boolean shouldOmitArgs(String uri) {
        if (uri == null || uri.isBlank()) {
            return false;
        }
        return NO_ARGS_URI_SUFFIXES.stream().anyMatch(uri::endsWith);
    }

    public boolean isProdProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch("prod"::equalsIgnoreCase);
    }

    public String sanitizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        return Arrays.stream(args)
                .map(this::sanitizeOne)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String sanitizeOne(Object arg) {
        if (arg == null) {
            return "null";
        }
        if (arg instanceof LoginDTO login) {
            return "LoginDTO(username=" + login.getUsername()
                    + ", password=***, roleCode=" + login.getRoleCode()
                    + ", mfaCode=" + (login.getMfaCode() != null ? "***" : "null") + ")";
        }
        if (arg instanceof RegisLoginDTO) {
            return "RegisLoginDTO(oldPassword=***, newPassword=***, confirmPassword=***)";
        }
        if (isProdProfile()) {
            return arg.getClass().getSimpleName();
        }
        return String.valueOf(arg);
    }
}
