package org.example.hrmanagement.common.aop;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.hrmanagement.common.annotation.OperationLog;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.system.mapper.OperationLogMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 操作日志切面：记录带有 @OperationLog 注解的方法调用，并落库。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final int PARAMS_MAX = 2000;

    private final OperationLogMapper operationLogMapper;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint point, OperationLog opLog) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        String ip = request != null ? request.getRemoteAddr() : "unknown";
        String uri = request != null ? request.getRequestURI() : "?";
        String httpMethod = request != null ? request.getMethod() : "";
        String module = opLog.module();
        String desc = opLog.value();
        String method = httpMethod + " " + uri;
        String params = truncate(formatArgs(point.getArgs()));

        Long userId = null;
        try {
            userId = SecurityUtil.getUserId();
        } catch (Exception ignored) {
            // 无登录上下文时仍可记录
        }

        long start = System.currentTimeMillis();
        try {
            Object result = point.proceed();
            long elapsed = System.currentTimeMillis() - start;
            persist(userId, module, desc, method, params, ip, 1, null, elapsed);
            log.info("[OP_LOG] module={}, desc={}, uri={}, ip={}, elapsed={}ms, status=SUCCESS",
                    module, desc, uri, ip, elapsed);
            return result;
        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - start;
            String err = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            persist(userId, module, desc, method, params, ip, 0, truncate(err), elapsed);
            log.warn("[OP_LOG] module={}, desc={}, uri={}, ip={}, elapsed={}ms, status=FAIL, error={}",
                    module, desc, uri, ip, elapsed, err);
            throw e;
        }
    }

    private void persist(
            Long userId,
            String module,
            String operation,
            String method,
            String params,
            String ip,
            int status,
            String errorMsg,
            long duration) {
        try {
            org.example.hrmanagement.module.system.entity.OperationLog entity =
                    new org.example.hrmanagement.module.system.entity.OperationLog();
            entity.setUserId(userId);
            entity.setModule(module);
            entity.setOperation(operation);
            entity.setMethod(method);
            entity.setParams(params);
            entity.setIp(ip);
            entity.setStatus(status);
            entity.setErrorMsg(errorMsg);
            entity.setDuration(duration);
            operationLogMapper.insert(entity);
        } catch (Exception ex) {
            log.warn("[OP_LOG] 落库失败: {}", ex.getMessage());
        }
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg == null) {
                        return "null";
                    }
                    if (arg instanceof MultipartFile file) {
                        return "file:" + file.getOriginalFilename();
                    }
                    if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse) {
                        return arg.getClass().getSimpleName();
                    }
                    return String.valueOf(arg);
                })
                .collect(Collectors.joining(", "));
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= PARAMS_MAX ? text : text.substring(0, PARAMS_MAX);
    }
}
