package com.polo.boot.validation.service;

import com.polo.boot.validation.annotation.IdCard;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IdCardValidationService implements ConstraintValidator<IdCard, String> {

    // 加权因子
    private static final int[] WEIGHT = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    // 校验码
    private static final char[] CHECK_CODE = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    private boolean allow15;
    private boolean strictCheck;

    @Override
    public void initialize(IdCard constraintAnnotation) {
        this.allow15 = constraintAnnotation.allow15();
        this.strictCheck = constraintAnnotation.strictCheck();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;  // @NotNull 处理空值
        }

        // 18位身份证
        if (value.length() == 18) {
            return validate18(value);
        }

        // 15位身份证
        if (allow15 && value.length() == 15) {
            return validate15(value);
        }

        return false;
    }

    private boolean validate18(String idCard) {
        // 格式检查：前17位数字，最后一位数字或X
        if (!idCard.matches("^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[\\dXx]$")) {
            return false;
        }

        // 校验码验证
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (idCard.charAt(i) - '0') * WEIGHT[i];
        }
        char checkCode = CHECK_CODE[sum % 11];

        return Character.toUpperCase(idCard.charAt(17)) == checkCode;
    }

    private boolean validate15(String idCard) {
        // 15位：6位地区 + 6位生日(yyMMdd) + 3位顺序码
        return idCard.matches("^[1-9]\\d{5}\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}$");
    }
}