package com.polo.boot.validation.service;

import com.polo.boot.validation.annotation.IPAddress;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class IPAddressValidationService implements ConstraintValidator<IPAddress, String> {

    private Set<IPAddress.IPVersion> versions;
    private boolean allowPrivate;
    private boolean allowLoopback;

    @Override
    public void initialize(IPAddress constraintAnnotation) {
        this.versions = new HashSet<>(Arrays.asList(constraintAnnotation.version()));
        this.allowPrivate = constraintAnnotation.allowPrivate();
        this.allowLoopback = constraintAnnotation.allowLoopback();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        // IPv4 校验
        if (versions.contains(IPAddress.IPVersion.V4)) {
            if (isValidIPv4(value)) {
                return true;
            }
        }

        // IPv6 校验
        if (versions.contains(IPAddress.IPVersion.V6)) {
            if (isValidIPv6(value)) {
                return true;
            }
        }

        setMessage(context, "IP地址格式不正确，要求: " +
                (versions.contains(IPAddress.IPVersion.V4) ? "IPv4 " : "") +
                (versions.contains(IPAddress.IPVersion.V6) ? "IPv6" : ""));
        return false;
    }

    private boolean isValidIPv4(String ip) {
        String[] parts = ip.split("\\.", -1);
        if (parts.length != 4) return false;

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) return false;
                // 检查前导零（如 01 不合法）
                if (part.length() > 1 && part.startsWith("0")) return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        // 检查特殊地址
        if (!allowLoopback && ip.startsWith("127.")) return false;
        if (!allowPrivate) {
            // 10.x.x.x, 172.16-31.x.x, 192.168.x.x
            if (ip.startsWith("10.") ||
                    ip.matches("^172\\.(1[6-9]|2[0-9]|3[01])\\..*") ||
                    ip.startsWith("192.168.")) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidIPv6(String ip) {
        // 支持压缩格式（::）
        if (ip.contains("::")) {
            int count = ip.split(":", -1).length;
            if (count > 8) return false;
        } else {
            String[] parts = ip.split(":");
            if (parts.length != 8) return false;
        }

        // 检查每个段
        String[] parts = ip.split(":", -1);
        for (String part : parts) {
            if (part.isEmpty()) continue; // :: 产生的空段
            if (part.length() > 4) return false;
            try {
                int num = Integer.parseInt(part, 16);
                if (num < 0 || num > 0xFFFF) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    private void setMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}