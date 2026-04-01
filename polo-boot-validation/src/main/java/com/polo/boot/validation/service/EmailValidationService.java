package com.polo.boot.validation.service;

import com.polo.boot.validation.annotation.Email;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.naming.Context;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.regex.Pattern;

public class EmailValidationService implements ConstraintValidator<Email, String> {

    // 更严格的正则：用户名@域名.后缀
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                    "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    private boolean allowNull;
    private Set<String> allowedDomains;
    private Set<String> deniedDomains;
    private boolean checkMx;

    @Override
    public void initialize(Email constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
        this.allowedDomains = new HashSet<>(Arrays.asList(constraintAnnotation.allowedDomains()));
        this.deniedDomains = new HashSet<>(Arrays.asList(constraintAnnotation.deniedDomains()));
        this.checkMx = constraintAnnotation.checkMx();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return allowNull;
        }

        // 1. 基础格式校验
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            setMessage(context, "邮箱格式不正确");
            return false;
        }

        // 2. 提取域名
        String domain = value.substring(value.lastIndexOf("@") + 1);

        // 3. 白名单检查
        if (!allowedDomains.isEmpty() && !allowedDomains.contains(domain)) {
            setMessage(context, "只允许以下域名: " + String.join(", ", allowedDomains));
            return false;
        }

        // 4. 黑名单检查
        if (deniedDomains.contains(domain)) {
            setMessage(context, "该域名邮箱不被允许: " + domain);
            return false;
        }

        // 5. MX 记录检查（可选，性能开销大）
        if (checkMx && !hasMxRecord(domain)) {
            setMessage(context, "邮箱域名不存在或无法接收邮件");
            return false;
        }

        return true;
    }

    private boolean hasMxRecord(String domain) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[]{"MX"});
            return attrs != null && attrs.get("MX") != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void setMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}