package com.polo.boot.security.annotation;


import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 防止重复提交
 */
@Target(ElementType.METHOD)              // ① 只能用在方法上
@Retention(RetentionPolicy.RUNTIME)      // ② 运行时保留，AOP需要
@Documented                               // ③ 生成Javadoc
public @interface PreventDuplicateSubmit {

    /**
     * 防重时间窗口（秒）
     * 在此时间内，相同Key的请求被视为重复提交
     */
    int interval() default 5;              // ④ 默认5秒

    /**
     * 提示信息
     */
    String message() default "操作过于频繁，请稍后再试";  // ⑤ 默认提示

    /**
     * 幂等Key生成策略
     */
    KeyStrategy strategy() default KeyStrategy.USER_AND_METHOD;  // ⑥ 默认策略

    /**
     * 自定义SpEL表达式（当strategy=CUSTOM时使用）
     * 用于从方法参数中提取唯一标识
     */
    String keyExpression() default "";      // ⑦ 自定义Key生成

    /**
     * 是否立即释放锁（方法执行完就删）
     * 默认false：等自然过期，防止快速重试
     * true适用于查询类幂等（同参数同结果）
     */
    boolean immediateRelease() default false;  // ⑧ 是否立即释放

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;  // ⑨ 默认秒
}

