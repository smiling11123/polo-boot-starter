package com.polo.boot.web.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 操作模块，如：订单管理、用户管理 */
    String module() default "other";

    /**
     * 操作类型
     */
    OperationType type() default OperationType.OTHER;

    /** 操作描述，支持 SpEL 表达式，如：'创建订单[' + #dto.orderNo + ']' */
    String desc() default "";

    /** 是否记录请求参数 */
    boolean logParams() default true;

    /** 请求参数脱敏字段（正则匹配），如：password、token */
    String[] paramMaskPatterns() default {"password", "token", "secret", "key"};

    /** 是否记录响应结果 */
    boolean logResult() default false;

    /** 响应结果脱敏字段 */
    String[] resultMaskPatterns() default {};

    /** 是否异步记录 */
    boolean async() default true;

    /** 操作日志级别 */
    LogLevel level() default LogLevel.INFO;

    /** 是否忽略特定异常（不记录错误日志） */
    Class<? extends Exception>[] ignoreExceptions() default {};
}


