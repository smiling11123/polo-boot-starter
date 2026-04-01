package com.polo.boot.validation.service;

import com.polo.boot.validation.annotation.CarNo;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class CarNoValidationService implements ConstraintValidator<CarNo, String> {

    // 普通车牌（蓝牌/黄牌）：省份简称 + 字母 + 5位字母数字
    private static final Pattern NORMAL_PATTERN = Pattern.compile(
            "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼]" +
                    "[A-HJ-NP-Z][A-HJ-NP-Z0-9]{4}[A-HJ-NP-Z0-9挂学警港澳]$"
    );

    // 新能源车牌：省份 + (D/F) + 6位
    private static final Pattern NEW_ENERGY_PATTERN = Pattern.compile(
            "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼]" +
                    "[A-HJ-NP-Z][DF][A-HJ-NP-Z0-9]{5}$"
    );

    // 新能源大型车：省份 + 6位 + (D/F)
    private static final Pattern NEW_ENERGY_LARGE_PATTERN = Pattern.compile(
            "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼]" +
                    "[A-HJ-NP-Z][A-HJ-NP-Z0-9]{5}[DF]$"
    );

    private CarNo.CarType type;

    @Override
    public void initialize(CarNo constraintAnnotation) {
        this.type = constraintAnnotation.type();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        // 去除空格，转大写
        String carNo = value.replaceAll("\\s", "").toUpperCase();

        boolean valid = switch (type) {
            case NEW_ENERGY ->
                    NEW_ENERGY_PATTERN.matcher(carNo).matches() ||
                            NEW_ENERGY_LARGE_PATTERN.matcher(carNo).matches();
            case NORMAL -> NORMAL_PATTERN.matcher(carNo).matches();
            case AUTO ->
                    NEW_ENERGY_PATTERN.matcher(carNo).matches() ||
                            NEW_ENERGY_LARGE_PATTERN.matcher(carNo).matches() ||
                            NORMAL_PATTERN.matcher(carNo).matches();
            default -> NORMAL_PATTERN.matcher(carNo).matches();
        };

        if (!valid) {
            String msg = switch (type) {
                case NEW_ENERGY -> "新能源车牌格式：省份简称+字母+D/F+5位，如粤B·D12345";
                case NORMAL -> "普通车牌格式：省份简称+字母+5位，如粤B·12345";
                default -> "车牌号格式不正确";
            };
            setMessage(context, msg);
        }

        return valid;
    }

    private void setMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}