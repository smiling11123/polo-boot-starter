package com.polo.boot.validation.annotation;

import com.polo.boot.validation.service.EmailValidationService;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EmailValidationService.class)
@Documented
public @interface Email {

    String message() default "邮箱格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 是否允许 null
     */
    boolean allowNull() default false;

    /**
     * 允许的域名白名单（为空则不限制）
     */
    String[] allowedDomains() default {};

    /**
     * 禁止的域名黑名单
     */
    String[] deniedDomains() default {"tempmail.com", "10minutemail.com"};

    /**
     * 是否必须存在 MX 记录（需要网络检查，性能开销大）
     */
    boolean checkMx() default false;
}