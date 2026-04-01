package com.polo.boot.validation.annotation;

import com.polo.boot.validation.service.IdCardValidationService;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IdCardValidationService.class)
@Documented
public @interface IdCard {

    String message() default "身份证格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 是否允许15位老身份证
     */
    boolean allow15() default true;

    /**
     * 是否校验行政区划代码（前6位）
     */
    boolean strictCheck() default false;
}