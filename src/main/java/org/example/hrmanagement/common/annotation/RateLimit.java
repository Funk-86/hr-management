package org.example.hrmanagement.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流注解。value=最大请求数，time=时间窗口，unit=时间单位。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /** 时间窗口内允许的最大请求数 */
    int value() default 10;
    /** 时间窗口 */
    long time() default 60;
    /** 时间单位 */
    TimeUnit unit() default TimeUnit.SECONDS;
}
