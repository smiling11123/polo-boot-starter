package com.polo.boot.web.spel;


import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SpelExpressionUtil {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();
    private static final ConcurrentMap<String, Expression> EXPRESSION_CACHE = new ConcurrentHashMap<>();

    /**
     * 创建 SpEL 上下文
     */
    public static EvaluationContext createContext(Method method, Object[] args) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 1. 设置方法参数
        String[] paramNames = NAME_DISCOVERER.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                // 参数名
                context.setVariable(paramNames[i], args[i]);
                // 多种索引兼容写法
                context.setVariable("arg" + i, args[i]);   // #arg0
                context.setVariable("a" + i, args[i]);     // #a0
                context.setVariable("p" + i, args[i]);     // #p0
            }
        } else {
            // 获取不到参数名，只用索引
            for (int i = 0; i < args.length; i++) {
                context.setVariable("arg" + i, args[i]);
                context.setVariable("a" + i, args[i]);
                context.setVariable("p" + i, args[i]);
            }
        }

        // 2. 设置工具方法
        context.setVariable("T", new SpelFunctions());

        return context;
    }

    /**
     * 添加返回值到上下文（方法执行后调用）
     */
    public static void addResult(EvaluationContext context, Object result) {
        context.setVariable("result", result);
    }

    /**
     * 添加异常到上下文
     */
    public static void addException(EvaluationContext context, Throwable error) {
        context.setVariable("error", error);
        context.setVariable("errorMsg", error != null ? error.getMessage() : null);
    }

    /**
     * 解析表达式
     */
    public static String parse(String expression, EvaluationContext context) {
        if (expression == null || expression.isBlank()) {
            return expression;
        }
        if (!expression.trim().startsWith("#") && !expression.contains("#")) {
            // 纯文本，直接返回
            return expression;
        }

        try {
            Expression exp = EXPRESSION_CACHE.computeIfAbsent(expression, PARSER::parseExpression);
            Object value = exp.getValue(context);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            // 解析失败，返回原表达式
            return "[SpEL解析失败: " + expression + "]";
        }
    }

    public static boolean referencesResult(String expression) {
        return containsVariableReference(expression, "result");
    }

    public static boolean referencesError(String expression) {
        return containsVariableReference(expression, "error")
                || containsVariableReference(expression, "errorMsg");
    }

    private static boolean containsVariableReference(String expression, String variable) {
        if (expression == null || expression.isBlank()) {
            return false;
        }
        return expression.contains("#" + variable);
    }

    /**
     * SpEL 内置工具函数
     */
    public static class SpelFunctions {
        public static String now() {
            return LocalDateTime.now().toString();
        }
        public static String uuid() {
            return java.util.UUID.randomUUID().toString().replace("-", "");
        }
        public static String mask(String str, int start, int end) {
            if (str == null || str.length() <= start + end) return str;
            return str.substring(0, start) + "****" + str.substring(str.length() - end);
        }
    }
}
