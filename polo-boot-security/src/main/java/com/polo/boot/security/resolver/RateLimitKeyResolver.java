package com.polo.boot.security.resolver;

import com.polo.boot.core.util.IpUtil;
import com.polo.boot.security.annotation.RateLimit;
import com.polo.boot.security.context.UserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RateLimitKeyResolver {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer DISCOVERER = new DefaultParameterNameDiscoverer();
    private static final ConcurrentMap<String, Expression> EXPRESSION_CACHE = new ConcurrentHashMap<>();

    public String resolve(ProceedingJoinPoint point, RateLimit rateLimit) {
        StringBuilder key = new StringBuilder("rate:");

        // 基础 key：类名.方法名
        MethodSignature signature = (MethodSignature) point.getSignature();
        String className = point.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();
        key.append(className).append(".").append(methodName);

        // 根据维度追加
        switch (rateLimit.type()) {
            case USER:
                String userId = getCurrentUserId();
                key.append(":user:").append(userId != null ? userId : "anonymous");
                break;

            case IP:
                String ip = getClientIp();
                key.append(":ip:").append(ip);
                break;

            case CUSTOM:
                if (StringUtils.hasText(rateLimit.key())) {
                    String customKey = evaluateSpel(rateLimit.key(), point);
                    key.append(":").append(customKey);
                }
                break;

            default:
                // 默认只按接口限流，不追加额外维度
        }

        return key.toString();
    }

    private String evaluateSpel(String expression, ProceedingJoinPoint point) {
        if (!StringUtils.hasText(expression)) {
            return "";
        }
        if (!expression.contains("#")) {
            return expression;
        }
        MethodSignature signature = (MethodSignature) point.getSignature();
        EvaluationContext context = new StandardEvaluationContext();

        // 设置参数
        String[] paramNames = DISCOVERER.getParameterNames(signature.getMethod());
        Object[] args = point.getArgs();
        for (int i = 0; i < args.length; i++) {
            if (paramNames != null && i < paramNames.length && StringUtils.hasText(paramNames[i])) {
                context.setVariable(paramNames[i], args[i]);
            }
            context.setVariable("p" + i, args[i]);
            context.setVariable("a" + i, args[i]);
        }

        Expression parsedExpression = EXPRESSION_CACHE.computeIfAbsent(expression, PARSER::parseExpression);
        return parsedExpression.getValue(context, String.class);
    }

    private String getCurrentUserId() {
        try {
            // 从 SecurityContext 或自定义上下文获取
            return UserContext.get().getPrincipalId().toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes();
            return IpUtil.getClientIp(attrs.getRequest());
        } catch (Exception e) {
            return "unknown";
        }
    }
}
