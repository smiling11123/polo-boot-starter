package com.polo.boot.security.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流维度
     */
    LimitType type() default LimitType.DEFAULT;

    /**
     * 限流 key 前缀（支持 SpEL）
     */
    String key() default "";

    /**
     * 限流速率（每秒请求数）
     */
    double rate() default 10;

    /**
     * 容量（令牌桶突发容量 / 窗口大小）
     */
    long capacity() default 100;

    /**
     * 限流算法
     */
    Algorithm algorithm() default Algorithm.TOKEN_BUCKET;

    /**
     * 限流策略
     */
    Strategy strategy() default Strategy.REJECT;

    /**
     * 等待超时时间（毫秒，仅 WAIT 策略有效）
     */
    long timeout() default 0;

    /**
     * 降级方法名（FALLBACK 策略）
     */
    String fallback() default "";

    /**
     * 错误提示消息
     */
    String message() default "请求过于频繁，请稍后再试";

    enum LimitType {
        DEFAULT,    // 全局限流（接口级）
        USER,       // 按用户限流
        IP,         // 按 IP 限流
        CUSTOM      // 自定义 key
    }

    enum Algorithm {
        TOKEN_BUCKET,   // 令牌桶：平滑限流，允许突发
        SLIDING_WINDOW, // 滑动窗口：精确统计，无突发
        FIXED_WINDOW    // 固定窗口：简单，但可能有临界突发
    }

    enum Strategy {
        REJECT,     // 直接拒绝，返回 429
        WAIT,       // 等待获取令牌
        FALLBACK    // 执行降级方法
    }
}