package org.example.hrmanagement.common.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.hrmanagement.common.annotation.RateLimit;
import org.example.hrmanagement.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 限流切面：基于 IP + 方法签名的内存限流。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    private final Map<String, RateLimitWindow> windows = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        String ip = request != null ? request.getRemoteAddr() : "unknown";
        String key = ip + ":" + point.getSignature().toLongString();

        RateLimitWindow window = windows.computeIfAbsent(key,
                k -> new RateLimitWindow(rateLimit.time(), rateLimit.unit()));

        if (!window.tryAcquire(rateLimit.value())) {
            log.warn("Rate limit exceeded: key={}, limit={}", key, rateLimit.value());
            throw new BusinessException("请求过于频繁，请稍后再试");
        }

        return point.proceed();
    }

    private static class RateLimitWindow {
        private final long windowMs;
        private final AtomicLong windowStart;
        private final AtomicInteger count;

        RateLimitWindow(long time, java.util.concurrent.TimeUnit unit) {
            this.windowMs = unit.toMillis(time);
            this.windowStart = new AtomicLong(System.currentTimeMillis());
            this.count = new AtomicInteger(0);
        }

        synchronized boolean tryAcquire(int max) {
            long now = System.currentTimeMillis();
            if (now - windowStart.get() > windowMs) {
                windowStart.set(now);
                count.set(0);
            }
            return count.incrementAndGet() <= max;
        }
    }
}
