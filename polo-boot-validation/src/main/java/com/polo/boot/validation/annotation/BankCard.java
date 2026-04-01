package com.polo.boot.validation.annotation;

import com.polo.boot.validation.service.BankCardValidationService;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BankCardValidationService.class)
@Documented
public @interface BankCard {
    String message() default "银行卡号格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
