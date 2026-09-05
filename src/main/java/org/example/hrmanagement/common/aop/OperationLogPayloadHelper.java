package org.example.hrmanagement.common.aop;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Operation log request/response serialize, sanitize and truncate.
 */
@Component
@RequiredArgsConstructor
public class OperationLogPayloadHelper {

    public static final int BODY_MAX = 8192;
    public static final int PARAMS_SUMMARY_MAX = 512;

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password",
            "oldpassword",
            "newpassword",
            "confirmpassword",
            "token",
            "accesstoken",
            "refreshtoken",
            "authorization",
            "mfacode",
            "secret",
            "descriptor"
    );

    private final ObjectMapper objectMapper;

    public String buildRequestInfo(HttpServletRequest request, Object[] args) {
        Map<String, Object> root = new LinkedHashMap<>();
        if (request != null) {
            String contentType = request.getContentType();
            if (contentType != null) {
                root.put("contentType", contentType);
            }
            root.put("query", toQueryMap(request));
        } else {
            root.put("query", Map.of());
        }
        root.put("body", toBodyValue(args));
        return truncate(writeJson(root), BODY_MAX);
    }

    public String buildResponseInfo(Object result) {
        if (result == null) {
            return "null";
        }
        if (isBinaryOrStream(result)) {
            return "[binary/stream omitted]";
        }
        if (result instanceof ResponseEntity<?> entity) {
            Object body = entity.getBody();
            if (body == null) {
                return "{\"status\":" + entity.getStatusCode().value() + ",\"body\":null}";
            }
            if (isBinaryOrStream(body)) {
                return "[binary/stream omitted]";
            }
            try {
                Object tree = objectMapper.convertValue(body, Object.class);
                Map<String, Object> root = new LinkedHashMap<>();
                root.put("status", entity.getStatusCode().value());
                root.put("body", sanitizeValue(tree));
                return truncate(writeJson(root), BODY_MAX);
            } catch (Exception e) {
                return truncate(String.valueOf(body), BODY_MAX);
            }
        }
        try {
            Object tree = objectMapper.convertValue(result, Object.class);
            return truncate(writeJson(sanitizeValue(tree)), BODY_MAX);
        } catch (Exception e) {
            return truncate(String.valueOf(result), BODY_MAX);
        }
    }

    private boolean isBinaryOrStream(Object value) {
        return value instanceof Resource
                || value instanceof StreamingResponseBody
                || value instanceof byte[];
    }

    public String buildErrorResponseInfo(Throwable e) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("error", e.getClass().getSimpleName());
        root.put("message", e.getMessage() == null ? "" : e.getMessage());
        return truncate(writeJson(root), BODY_MAX);
    }

    public String buildParamsSummary(Object[] args) {
        return truncate(writeJson(toBodyValue(args)), PARAMS_SUMMARY_MAX);
    }

    private Map<String, Object> toQueryMap(HttpServletRequest request) {
        Map<String, Object> query = new LinkedHashMap<>();
        Map<String, String[]> map = request.getParameterMap();
        if (map == null || map.isEmpty()) {
            return query;
        }
        map.forEach((key, values) -> {
            if (values == null) {
                query.put(key, null);
            } else if (values.length == 1) {
                query.put(key, maskIfSensitive(key, values[0]));
            } else {
                List<String> list = new ArrayList<>(values.length);
                for (String v : values) {
                    list.add(maskIfSensitive(key, v));
                }
                query.put(key, list);
            }
        });
        return query;
    }

    private Object toBodyValue(Object[] args) {
        if (args == null || args.length == 0) {
            return List.of();
        }
        List<Object> list = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null) {
                list.add(null);
                continue;
            }
            if (arg instanceof HttpServletRequest
                    || arg instanceof HttpServletResponse
                    || arg instanceof BindingResult) {
                continue;
            }
            if (arg instanceof MultipartFile file) {
                Map<String, Object> fileNode = new LinkedHashMap<>();
                fileNode.put("fileName", file.getOriginalFilename());
                fileNode.put("size", file.getSize());
                fileNode.put("contentType", file.getContentType());
                list.add(fileNode);
                continue;
            }
            if (arg instanceof MultipartFile[] files) {
                List<Object> filesNode = new ArrayList<>();
                for (MultipartFile file : files) {
                    if (file == null) {
                        continue;
                    }
                    Map<String, Object> fileNode = new LinkedHashMap<>();
                    fileNode.put("fileName", file.getOriginalFilename());
                    fileNode.put("size", file.getSize());
                    filesNode.add(fileNode);
                }
                list.add(filesNode);
                continue;
            }
            try {
                Object converted = objectMapper.convertValue(arg, Object.class);
                list.add(sanitizeValue(converted));
            } catch (Exception e) {
                list.add(arg.getClass().getSimpleName());
            }
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private Object sanitizeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            map.forEach((k, v) -> {
                String key = String.valueOf(k);
                if (isSensitiveKey(key)) {
                    out.put(key, "***");
                } else if (v instanceof String s && isOmittedLargeText(s)) {
                    out.put(key, "[omitted]");
                } else if (v instanceof byte[]) {
                    out.put(key, "[omitted:binary]");
                } else {
                    out.put(key, sanitizeValue(v));
                }
            });
            return out;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> out = new ArrayList<>(collection.size());
            for (Object item : collection) {
                out.add(sanitizeValue(item));
            }
            return out;
        }
        if (value instanceof Object[] array) {
            List<Object> out = new ArrayList<>(array.length);
            for (Object item : array) {
                out.add(sanitizeValue(item));
            }
            return out;
        }
        if (value instanceof byte[]) {
            return "[omitted:binary]";
        }
        if (value instanceof String s && isOmittedLargeText(s)) {
            return "[omitted]";
        }
        return value;
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        return SENSITIVE_KEYS.contains(key.toLowerCase(Locale.ROOT).replace("_", ""));
    }

    private String maskIfSensitive(String key, String value) {
        return isSensitiveKey(key) ? "***" : value;
    }

    private boolean isOmittedLargeText(String text) {
        if (text == null || text.length() < 500) {
            return false;
        }
        int sample = Math.min(text.length(), 80);
        String head = text.substring(0, sample);
        long base64ish = head.chars().filter(c ->
                (c >= 'A' && c <= 'Z')
                        || (c >= 'a' && c <= 'z')
                        || (c >= '0' && c <= '9')
                        || c == '+' || c == '/' || c == '=').count();
        return base64ish >= sample * 0.9;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[serialize error]";
        }
    }

    public String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "...(truncated, total=" + text.length() + ")";
    }
}
