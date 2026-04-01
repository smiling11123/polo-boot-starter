package com.polo.boot.validation.annotation;

import com.polo.boot.validation.service.PhoneValidationService;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})  // ① 可用在字段和参数上
@Retention(RetentionPolicy.RUNTIME)                   // ② 运行时保留
@Constraint(validatedBy = PhoneValidationService.class)       // ③ 指定校验器类
@Documented
public @interface Phone {
    String message() default "手机号格式不正确";

    boolean required() default true; //是否是必填属性

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
