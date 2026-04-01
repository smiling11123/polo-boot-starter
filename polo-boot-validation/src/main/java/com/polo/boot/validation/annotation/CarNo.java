package com.polo.boot.validation.annotation;

import com.polo.boot.validation.service.CarNoValidationService;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CarNoValidationService.class)
@Documented
public @interface CarNo {

    String message() default "车牌号格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 车牌类型
     */
    CarType type() default CarType.AUTO;

    enum CarType {
        AUTO,       // 自动识别
        NEW_ENERGY, // 新能源（绿牌）
        NORMAL,     // 普通车牌（蓝牌/黄牌）
        HONG_KONG,  // 港牌
        MACAO,      // 澳牌
        ARMY,       // 军牌
        POLICE      // 警牌
    }
}
