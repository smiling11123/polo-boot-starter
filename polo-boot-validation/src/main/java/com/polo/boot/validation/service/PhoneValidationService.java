package com.polo.boot.validation.service;

import com.polo.boot.validation.annotation.Phone;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PhoneValidationService implements ConstraintValidator<Phone, String> {
    private static final Pattern PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private Boolean required;

    @Override
    public void initialize(Phone annotation){
        this.required = annotation.required();
    }
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return !required;
        }
        return PATTERN.matcher(value).matches();
    }
}
