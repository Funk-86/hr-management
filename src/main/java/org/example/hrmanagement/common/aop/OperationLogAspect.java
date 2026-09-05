package org.example.hrmanagement.common.aop;

import jakarta.servlet.http.HttpServletRequest;
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

/**
 * 操作日志切面：记录带有 @OperationLog 注解的方法调用，并落库 Request / Response。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogMapper operationLogMapper;
    private final OperationLogPayloadHelper payloadHelper;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint point, OperationLog opLog) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        String ip = resolveClientIp(request);
        String uri = request != null ? request.getRequestURI() : "?";
        String httpMethod = request != null ? request.getMethod() : "";
        String module = opLog.module();
        String desc = opLog.value();
        String method = httpMethod + " " + uri;
        String requestInfo = payloadHelper.buildRequestInfo(request, point.getArgs());
        String params = payloadHelper.buildParamsSummary(point.getArgs());

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
            String responseInfo = payloadHelper.buildResponseInfo(result);
            persist(userId, module, desc, method, params, requestInfo, responseInfo, ip, 1, null, elapsed);
            log.info("[OP_LOG] module={}, desc={}, uri={}, ip={}, elapsed={}ms, status=SUCCESS",
                    module, desc, uri, ip, elapsed);
            return result;
        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - start;
            String err = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            String responseInfo = payloadHelper.buildErrorResponseInfo(e);
            persist(userId, module, desc, method, params, requestInfo, responseInfo, ip, 0,
                    payloadHelper.truncate(err, OperationLogPayloadHelper.BODY_MAX), elapsed);
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
            String requestInfo,
            String responseInfo,
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
            entity.setRequestInfo(requestInfo);
            entity.setResponseInfo(responseInfo);
            entity.setIp(ip);
            entity.setStatus(status);
            entity.setErrorMsg(errorMsg);
            entity.setDuration(duration);
            operationLogMapper.insert(entity);
        } catch (Exception ex) {
            log.warn("[OP_LOG] 落库失败: {}", ex.getMessage());
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
