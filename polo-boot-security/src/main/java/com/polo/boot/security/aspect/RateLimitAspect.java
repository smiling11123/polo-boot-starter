package com.polo.boot.security.aspect;

import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.core.model.Result;
import com.polo.boot.security.annotation.RateLimit;
import com.polo.boot.security.resolver.RateLimitKeyResolver;
import com.polo.boot.security.support.RedisRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Order(0)
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisRateLimiter rateLimiter;
    private final RateLimitKeyResolver keyResolver;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        // 1. 解析限流 key
        String key = keyResolver.resolve(point, rateLimit);

        // 2. 尝试获取许可
        boolean acquired;
        if (rateLimit.strategy() == RateLimit.Strategy.WAIT && rateLimit.timeout() > 0) {
            acquired = rateLimiter.tryAcquireWithWait(key, rateLimit, rateLimit.timeout());
        } else {
            acquired = rateLimiter.tryAcquire(key, rateLimit);
        }

        // 3. 处理结果
        if (acquired) {
            // 限流通过，执行原方法
            return point.proceed();
        }

        // 4. 限流触发
        log.warn("[限流触发] key={}, rate={}/s", key, rateLimit.rate());

        return handleLimitExceeded(point, rateLimit);
    }

    private Object handleLimitExceeded(ProceedingJoinPoint point, RateLimit rateLimit) {
        switch (rateLimit.strategy()) {
            case REJECT:
                throw new BizException(ErrorCode.RATE_LIMIT);

            case FALLBACK:
                return executeFallback(point, rateLimit.fallback());

            case WAIT:
                // 理论上不会走到这里（前面已经等待过了）
                throw new BizException(ErrorCode.RATE_LIMIT);

            default:
                throw new BizException(ErrorCode.RATE_LIMIT);
        }
    }

    private Object executeFallback(ProceedingJoinPoint point, String fallbackMethod) {
        if (!StringUtils.hasText(fallbackMethod)) {
            return Result.fail(429, "服务繁忙，请稍后再试");
        }

        try {
            // 反射调用降级方法
            Object target = point.getTarget();
            Method method = target.getClass().getMethod(fallbackMethod);
            return method.invoke(target);
        } catch (Exception e) {
            log.error("执行降级方法失败", e);
            return Result.fail(429, "服务繁忙");
        }
    }
}
