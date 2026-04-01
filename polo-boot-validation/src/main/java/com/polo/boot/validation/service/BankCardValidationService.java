package com.polo.boot.validation.service;

import com.polo.boot.validation.annotation.BankCard;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BankCardValidationService implements ConstraintValidator<BankCard, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;

        // 去除空格
        String cardNo = value.replaceAll("\\s", "");

        // 长度检查（13-19位）
        if (!cardNo.matches("^\\d{13,19}$")) {
            return false;
        }

        // Luhn 算法校验
        return luhnCheck(cardNo);
    }

    private boolean luhnCheck(String cardNo) {
        int sum = 0;
        boolean alternate = false;

        for (int i = cardNo.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(cardNo.charAt(i));
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }
}
