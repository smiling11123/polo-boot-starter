package com.polo.boot.validation.service;

import com.polo.boot.validation.annotation.CreditCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CreditCodeValidationService implements ConstraintValidator<CreditCode, String> {
    // 统一社会信用代码字符集（不含I、O、Z、S、V）
    private static final String BASE_CODE_STRING = "0123456789ABCDEFGHJKLMNPQRTUWXY";

    // 各位置权重（加权因子）
    private static final int[] WEIGHTS = {
            1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28
    };

    // 模数
    private static final int MOD = 31;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;  // @NotNull 处理空值
        }

        // 1. 基础格式校验
        if (!value.matches("^[0-9A-HJ-NPQRTUWXY]{18}$")) {
            setMessage(context, "统一社会信用代码必须是18位，且不含I、O、Z、S、V");
            return false;
        }

        // 2. 校验码验证
        if (!checkCode(value)) {
            setMessage(context, "统一社会信用代码校验位错误");
            return false;
        }

        return true;
    }

    /**
     * 校验码计算核心方法
     */
    private boolean checkCode(String creditCode) {
        // 前17位
        String pre17 = creditCode.substring(0, 17);
        // 第18位（校验码）
        char checkCode = creditCode.charAt(17);

        // 计算期望的校验码
        char expectedCheckCode = calculateCheckCode(pre17);

        return checkCode == expectedCheckCode;
    }

    /**
     * 计算校验码
     */
    private char calculateCheckCode(String pre17) {
        int sum = 0;

        // 遍历前17位，计算加权和
        for (int i = 0; i < pre17.length(); i++) {
            char c = pre17.charAt(i);
            // 字符对应的数值（在BASE_CODE_STRING中的索引）
            int value = BASE_CODE_STRING.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("非法字符: " + c);
            }
            // 加权和
            sum += value * WEIGHTS[i];
        }

        // 计算校验码索引
        int checkCodeIndex = MOD - (sum % MOD);
        if (checkCodeIndex == MOD) {
            checkCodeIndex = 0;
        }

        return BASE_CODE_STRING.charAt(checkCodeIndex);
    }

    /**
     * 设置自定义错误消息
     */
    private void setMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}