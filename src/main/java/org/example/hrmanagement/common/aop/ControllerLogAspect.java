package org.example.hrmanagement.common.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 控制器日志切面：自动记录请求入参、响应、耗时。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ControllerLogAspect {

    private final LogSanitizer logSanitizer;

    @Pointcut("execution(public * org.example.hrmanagement.module..controller..*(..))")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        String method = request != null ? request.getMethod() : "?";
        String uri = request != null ? request.getRequestURI() : "?";
        String className = point.getSignature().getDeclaringType().getSimpleName();
        String methodName = point.getSignature().getName();
        String argsText = formatArgs(uri, point.getArgs());

        log.info("[{}] {}.{} | method={} | args={}", uri, className, methodName, method, argsText);

        long start = System.currentTimeMillis();
        Object result = point.proceed();
        long elapsed = System.currentTimeMillis() - start;

        log.info("[{}] {}.{} | elapsed={}ms | status=OK", uri, className, methodName, elapsed);
        return result;
    }

    private String formatArgs(String uri, Object[] args) {
        if (logSanitizer.shouldOmitArgs(uri)) {
            return "[omitted]";
        }
        if (logSanitizer.isProdProfile()) {
            return logSanitizer.sanitizeArgs(args);
        }
        return logSanitizer.sanitizeArgs(args);
    }
}
