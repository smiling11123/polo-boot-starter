package com.polo.boot.validation.annotation;

import com.polo.boot.validation.service.IPAddressValidationService;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IPAddressValidationService.class)
@Documented
public @interface IPAddress {

    String message() default "IP地址格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 允许的 IP 版本
     */
    IPVersion[] version() default {IPVersion.V4, IPVersion.V6};

    /**
     * 是否允许私有地址（内网IP）
     */
    boolean allowPrivate() default true;

    /**
     * 是否允许回环地址（127.0.0.1）
     */
    boolean allowLoopback() default true;

    enum IPVersion {
        V4, V6
    }
}

