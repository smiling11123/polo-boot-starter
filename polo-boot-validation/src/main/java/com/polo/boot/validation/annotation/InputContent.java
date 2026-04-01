package com.polo.boot.validation.annotation;

import com.polo.boot.validation.service.InputContentValidationService;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = InputContentValidationService.class)
@Documented
public @interface InputContent {

    String message() default "内容包含不当言论";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 检测类型
     */
    CheckType[] types() default {CheckType.SENSITIVE_WORD};

    /**
     * 敏感词分类编码（支持用户自定义分类，为空则检测所有）。
     * 建议优先使用该字段；旧的 categories 仅保留兼容。
     */
    String[] categoryCodes() default {};

    /**
     * 处理策略
     */
    Strategy strategy() default Strategy.REJECT;

    /**
     * 脱敏替换符（MASK策略有效）
     */
    String maskChar() default "*";

    /**
     * 自定义敏感词（额外添加）
     */
    String[] customWords() default {};

    /**
     * 忽略词（白名单）
     */
    String[] ignoreWords() default {};

    /**
     * 是否检测联系方式泄露
     */
    boolean checkContactLeak() default true;

    /**
     * 是否启用 AI 检测（需要对接 NLP 服务）
     */
    boolean enableAiCheck() default false;

    /**
     * AI 检测阈值（0-1，越高越严格）
     */
    double aiThreshold() default 0.8;

    enum CheckType {
        SENSITIVE_WORD,     // 敏感词匹配
        REGEX_PATTERN,      // 正则规则（联系方式、证件号）
        SEMANTIC_ANALYSIS,  // 语义分析（AI）
        CUSTOM_RULE         // 自定义规则
    }

    enum Strategy {
        REJECT,     // 拒绝，抛异常
        MASK,       // 脱敏替换
        REPLACE,    // 智能替换
        REVIEW,     // 标记审核
        LOG         // 仅记录日志
    }

    final class CategoryCodes {
        public static final String POLITICS = "政治敏感";
        public static final String PORNOGRAPHY = "色情低俗";
        public static final String VIOLENCE = "暴力恐怖";
        public static final String DISCRIMINATION = "歧视辱骂";
        public static final String GAMBLING = "赌博诈骗";
        public static final String DRUGS = "毒品违禁";
        public static final String PRIVACY = "隐私信息";
        public static final String ADVERTISING = "广告垃圾";

        private CategoryCodes() {
        }
    }
}
