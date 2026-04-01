package com.polo.boot.security.aspect;

import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.security.annotation.PreventDuplicateSubmit;
import com.polo.boot.security.key.KeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

@Aspect
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DuplicateSubmitAspect {

    private final StringRedisTemplate redisTemplate;

    //  Lua脚本：原子性检查+设置，避免并发漏洞
    // KEYS[1]是Key，ARGV[1]是过期时间（秒）
    private static final String LOCK_SCRIPT = """
        if redis.call('exists', KEYS[1]) == 1 then
            return 0;  -- Key已存在，获取锁失败（重复提交）
        else
            redis.call('setex', KEYS[1], ARGV[1], '1');  -- 设置Key+过期时间
            return 1;  -- 获取锁成功
        end
        """;

    private final DefaultRedisScript<Long> lockScript =
            new DefaultRedisScript<>(LOCK_SCRIPT, Long.class);

    @Around(value = "@annotation(preventDuplicateSubmit)", argNames = "point,preventDuplicateSubmit")
    public Object duplicateSubmit(ProceedingJoinPoint point, PreventDuplicateSubmit preventDuplicateSubmit) throws Throwable {
        //生成唯一key
        String key = KeyGenerator.generate(point, preventDuplicateSubmit);
        //缓存过期时间
        long expireSeconds = preventDuplicateSubmit.timeUnit()
                .toSeconds(preventDuplicateSubmit.interval());
        //是否获取到锁
        Long result = redisTemplate.execute(lockScript, Collections.singletonList(key), String.valueOf(expireSeconds));

        if (result == null || result == 0) {
            throw new BizException(ErrorCode.REPEAT_SUBMIT.getCode(), preventDuplicateSubmit.message());
        }
        try {
            return point.proceed();
        } finally {
            //  方法执行完毕，处理锁释放
            if (preventDuplicateSubmit.immediateRelease()) {
                //  立即释放（适用于查询类幂等）
                redisTemplate.delete(key);
                log.debug("[防重提交] 立即释放锁，key={}", key);
            }
            //  否则：等Redis自然过期（默认，防止快速重试）
        }
    }
}
