package com.polo.boot.security.key;

import com.polo.boot.security.annotation.KeyStrategy;
import com.polo.boot.security.annotation.PreventDuplicateSubmit;
import com.polo.boot.security.context.UserContext;
import com.polo.boot.core.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.springframework.util.StringUtils.hasText;

/**
 * 幂等Key生成器
 */
public class KeyGenerator {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final String PREFIX = "idempotent:";  // Redis Key前缀

    /**
     * 生成唯一Key
     */
    public static String generate(ProceedingJoinPoint point,
                                  PreventDuplicateSubmit annotation) {

        // ① 获取策略
        KeyStrategy strategy = annotation.strategy();

        // ② 根据策略生成原始Key
        String rawKey = switch (strategy) {
            case USER_AND_METHOD -> generateUserAndMethodKey(point);
            case USER_AND_URI -> generateUserAndUriKey();
            case METHOD_ONLY -> generateMethodOnlyKey(point);
            case CUSTOM -> generateCustomKey(point, annotation.keyExpression());
        };

        // ③ 添加前缀，避免和其他业务Key冲突
        // ④ MD5压缩（防止Key过长）
        return PREFIX + md5(rawKey);
    }

    /**
     * 策略1：用户ID + 方法签名
     * 效果：同一个用户，同一个方法，在interval秒内只能调用一次
     *
     * 示例：用户A调用OrderController.create()，5秒内不能再调
     *       但用户A可以调用OrderController.update()，用户B可以调用create()
     */
    private static String generateUserAndMethodKey(ProceedingJoinPoint point) {
        // ⑤ 获取用户ID（未登录用IP代替，或抛异常）
        String userId = getCurrentUserId();

        // ⑥ 获取方法签名：类名+方法名+参数类型
        MethodSignature signature = (MethodSignature) point.getSignature();
        String methodSign = signature.getDeclaringTypeName() + "."
                + signature.getName() + "."
                + signature.getParameterTypes().length;

        // ⑦ 组合：用户维度 + 方法维度
        return userId + ":" + methodSign;
    }

    /**
     * 策略2：用户ID + 请求URI
     * 效果：同一个用户，同一个URI，在interval秒内只能调用一次
     *
     * 与USER_AND_METHOD区别：URI相同但方法不同（如GET/POST）也会防重
     */
    private static String generateUserAndUriKey() {
        String userId = getCurrentUserId();

        // ⑧ 获取当前请求URI
        HttpServletRequest request = getCurrentRequest();
        String uri = request != null ? request.getRequestURI() : "unknown";

        return userId + ":" + uri;
    }

    /**
     * 策略3：仅方法签名
     * 效果：所有用户共享同一个防重锁
     *
     * ⚠️ 慎用！适合全局限制场景，如系统初始化、定时任务触发
     */
    private static String generateMethodOnlyKey(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        return signature.getDeclaringTypeName() + "." + signature.getName();
    }

    /**
     * 策略4：自定义SpEL表达式
     * 效果：根据业务参数生成Key，最灵活
     *
     * 示例：keyExpression = "#dto.orderNo" → 同一订单号防重
     *       keyExpression = "#userId + ':' + #dto.type" → 用户+类型组合
     */
    private static String generateCustomKey(ProceedingJoinPoint point,
                                            String expression) {
        if (!hasText(expression)) {
            throw new IllegalArgumentException("CUSTOM策略必须指定keyExpression");
        }

        // ⑨ 构建SpEL上下文
        StandardEvaluationContext context = new StandardEvaluationContext();

        // ⑩ 放入方法参数
        MethodSignature signature = (MethodSignature) point.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = point.getArgs();

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        // ⑪ 放入用户ID（方便直接用#userId）
        context.setVariable("userId", getCurrentUserId());

        // ⑫ 解析表达式
        Object value = PARSER.parseExpression(expression).getValue(context);

        return value != null ? value.toString() : "null";
    }

    // ========== 工具方法 ==========

    private static String getCurrentUserId() {
        try {
            if(UserContext.get() != null){
                return UserContext.get().getPrincipalId().toString();
            }
        } catch (Exception ignored) {}

        // ⑬ 未登录场景：用IP+UA指纹代替，或抛异常强制登录
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String ip = IpUtil.getClientIp(request);
            String ua = request.getHeader("User-Agent");
            return "ip:" + md5(ip + ua).substring(0, 8);
        }

        throw new IllegalStateException("无法获取用户标识，请登录或检查请求");
    }

    private static HttpServletRequest getCurrentRequest() {
        try {
            return ((ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes()).getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private static String md5(String str) {
        // MD5实现...
        return DigestUtils.md5DigestAsHex(str.getBytes());
    }
}