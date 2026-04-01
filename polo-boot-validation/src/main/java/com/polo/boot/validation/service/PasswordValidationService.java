package com.polo.boot.validation.service;

import com.polo.boot.validation.annotation.Password;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PasswordValidationService implements ConstraintValidator<Password, String> {

    private int min;
    private int max;
    private boolean requireDigit;
    private boolean requireLower;
    private boolean requireUpper;
    private boolean requireSpecial;
    private String specialChars;
    private boolean notContainUsername;
    private boolean noSequential;
    private int minStrength;

    @Override
    public void initialize(Password constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
        this.requireDigit = constraintAnnotation.requireDigit();
        this.requireLower = constraintAnnotation.requireLower();
        this.requireUpper = constraintAnnotation.requireUpper();
        this.requireSpecial = constraintAnnotation.requireSpecial();
        this.specialChars = constraintAnnotation.specialChars();
        this.notContainUsername = constraintAnnotation.notContainUsername();
        this.noSequential = constraintAnnotation.noSequential();
        this.minStrength = constraintAnnotation.minStrength();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        // 长度检查
        if (value.length() < min || value.length() > max) {
            setMessage(context, "密码长度必须在" + min + "-" + max + "位之间");
            return false;
        }

        // 复杂度检查
        boolean hasDigit = value.matches(".*\\d.*");
        boolean hasLower = value.matches(".*[a-z].*");
        boolean hasUpper = value.matches(".*[A-Z].*");
        boolean hasSpecial = value.matches(".*[" + Pattern.quote(specialChars) + "].*");

        if (requireDigit && !hasDigit) {
            setMessage(context, "密码必须包含数字");
            return false;
        }
        if (requireLower && !hasLower) {
            setMessage(context, "密码必须包含小写字母");
            return false;
        }
        if (requireUpper && !hasUpper) {
            setMessage(context, "密码必须包含大写字母");
            return false;
        }
        if (requireSpecial && !hasSpecial) {
            setMessage(context, "密码必须包含特殊字符: " + specialChars);
            return false;
        }

        // 连续字符检查
        if (noSequential && hasSequential(value)) {
            setMessage(context, "密码不能包含连续字符（如123, abc）");
            return false;
        }

        // 强度评分
        int strength = calculateStrength(value, hasDigit, hasLower, hasUpper, hasSpecial);
        if (strength < minStrength) {
            setMessage(context, "密码强度不足，当前等级" + strength + "，要求最低" + minStrength);
            return false;
        }

        return true;
    }

    private boolean hasSequential(String value) {
        // 检查连续数字
        for (int i = 0; i < value.length() - 2; i++) {
            char c1 = value.charAt(i);
            char c2 = value.charAt(i + 1);
            char c3 = value.charAt(i + 2);

            // 连续数字 012 123 234 ...
            if (Character.isDigit(c1) && Character.isDigit(c2) && Character.isDigit(c3)) {
                if (c2 == c1 + 1 && c3 == c2 + 1) return true;
            }

            // 连续字母 abc bcd ...
            if (Character.isLetter(c1) && Character.isLetter(c2) && Character.isLetter(c3)) {
                char lower1 = Character.toLowerCase(c1);
                char lower2 = Character.toLowerCase(c2);
                char lower3 = Character.toLowerCase(c3);
                if (lower2 == lower1 + 1 && lower3 == lower2 + 1) return true;
            }
        }
        return false;
    }

    private int calculateStrength(String value, boolean digit, boolean lower,
                                  boolean upper, boolean special) {
        int score = 0;
        if (digit) score++;
        if (lower) score++;
        if (upper) score++;
        if (special) score++;

        // 长度加分
        if (value.length() >= 12) score++;
        if (value.length() >= 16) score++;

        return Math.min(score, 5);
    }

    private void setMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}