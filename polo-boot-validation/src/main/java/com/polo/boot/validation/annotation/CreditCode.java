package com.polo.boot.validation.annotation;

import com.polo.boot.validation.service.CreditCodeValidationService;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CreditCodeValidationService.class)
@Documented
public @interface CreditCode {
    String message() default "统一社会信用代码格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
