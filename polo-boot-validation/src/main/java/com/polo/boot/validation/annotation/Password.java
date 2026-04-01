package com.polo.boot.validation.annotation;

import com.polo.boot.validation.service.PasswordValidationService;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidationService.class)
@Documented
public @interface Password {

    String message() default "密码强度不符合要求";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 最小长度
     */
    int min() default 8;

    /**
     * 最大长度
     */
    int max() default 32;

    /**
     * 要求包含数字
     */
    boolean requireDigit() default true;

    /**
     * 要求包含小写字母
     */
    boolean requireLower() default true;

    /**
     * 要求包含大写字母
     */
    boolean requireUpper() default false;

    /**
     * 要求包含特殊字符
     */
    boolean requireSpecial() default false;

    /**
     * 特殊字符集合（默认常用特殊字符）
     */
    String specialChars() default "!@#$%^&*()_+-=[]{}|;:,.<>?";

    /**
     * 不能包含用户名
     */
    boolean notContainUsername() default true;

    /**
     * 不能包含连续字符（如123, abc）
     */
    boolean noSequential() default true;

    /**
     * 强度等级：1-弱 2-中 3-强 4-极强
     */
    int minStrength() default 2;
}