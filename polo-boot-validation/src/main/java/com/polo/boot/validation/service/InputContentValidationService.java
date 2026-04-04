package com.polo.boot.validation.service;

import com.polo.boot.validation.annotation.InputContent;
import com.polo.boot.validation.model.ContentValidationRecord;
import com.polo.boot.validation.model.RegexMatch;
import com.polo.boot.validation.properties.ValidationProperties;
import com.polo.boot.validation.util.StringUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class InputContentValidationService implements ConstraintValidator<InputContent, String> {
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern ID_CARD_CANDIDATE_PATTERN = Pattern.compile("\\d{17}[\\dXx]|\\d{15}");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("\\d{16,19}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    private final IdCardValidationService idCardValidationService = new IdCardValidationService();
    private final BankCardValidationService bankCardValidationService = new BankCardValidationService();
    private final SensitiveWordManager wordManager;
    private final AiContentChecker aiChecker;
    private final ValidationProperties properties;
    private final ContentValidationRecorder recorder;

    private InputContent annotation;
    private Set<String> categories;
    private Set<String> ignoreWords;
    private Set<InputContent.CheckType> checkTypes;

    @Override
    public void initialize(InputContent constraintAnnotation) {
        this.annotation = constraintAnnotation;
        this.categories = new HashSet<>();
        Arrays.stream(constraintAnnotation.categoryCodes())
                .map(this::normalizeCategoryCode)
                .filter(Objects::nonNull)
                .forEach(this.categories::add);
        this.ignoreWords = Arrays.stream(constraintAnnotation.ignoreWords())
                .map(String::trim)
                .filter(word -> !word.isEmpty())
                .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
        this.checkTypes = constraintAnnotation.types().length == 0
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(constraintAnnotation.types())));
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!properties.isEnabled() || !properties.getInputContent().isEnabled()) {
            return true;
        }
        if (value == null || value.isEmpty()) {
            return true;
        }

        // 1. 敏感词检测
        if (checkTypes.contains(InputContent.CheckType.SENSITIVE_WORD)) {
            List<SensitiveWordManager.MatchResult> matches = wordManager.check(value);

            // 过滤分类
            if (!categories.isEmpty()) {
                matches = matches.stream()
                        .filter(m -> categories.contains(normalizeCategoryCode(m.getCategory())))
                        .collect(Collectors.toList());
            }

            // 过滤自定义白名单
            matches = filterIgnoreWords(matches);

            if (!matches.isEmpty()) {
                return handleViolation(value, matches, context);
            }
        }

        // 2. 正则检测（联系方式泄露）
        if (checkTypes.contains(InputContent.CheckType.REGEX_PATTERN)
                && annotation.checkContactLeak()) {

            List<RegexMatch> regexMatches = checkRegexPatterns(value, context);
            if (!regexMatches.isEmpty()) {
                return handleRegexViolation(regexMatches, context);
            }
        }

        // 3. AI 语义检测
        if (checkTypes.contains(InputContent.CheckType.SEMANTIC_ANALYSIS)
                && annotation.enableAiCheck()) {

            double threshold = annotation.aiThreshold() > 0
                    ? annotation.aiThreshold()
                    : properties.getInputContent().getDefaultAiThreshold();
            AiContentChecker.AiCheckResult aiResult = aiChecker.check(value, threshold);
            if (aiResult.isSensitive()) {
                return handleAiViolation(aiResult, context);
            }
        }

        return true;
    }

    /**
     * 处理违规内容
     */
    private boolean handleViolation(String value,
                                    List<SensitiveWordManager.MatchResult> matches,
                                    ConstraintValidatorContext context) {

        String sensitiveWords = matches.stream()
                .map(SensitiveWordManager.MatchResult::getWord)
                .collect(Collectors.joining(", "));

        switch (annotation.strategy()) {
            case REJECT:
                record("SENSITIVE_WORD", "内容包含敏感词: " + sensitiveWords, value, sensitiveWords, matches.size());
                setMessage(context, "内容包含敏感词: " + sensitiveWords);
                return false;

            case MASK:
                // 脱敏处理（不返回 false，允许通过但内容已处理）
                String masked = wordManager.replace(value, annotation.maskChar());
                // 通过反射替换原值（需要特殊处理）
                context.unwrap(HibernateConstraintValidatorContext.class)
                        .withDynamicPayload(masked);
                record("SENSITIVE_WORD", "内容已脱敏处理", value, sensitiveWords, matches.size());
                return true;

            case REVIEW:
                // 标记待审核，记录日志
                record("SENSITIVE_WORD", "内容命中敏感词，已标记待审核", value, sensitiveWords, matches.size());
                log.warn("[内容待审核] 敏感词: {}, 内容: {}", sensitiveWords,
                        StringUtils.abbreviate(value, 100));
                return true;

            case LOG:
                record("SENSITIVE_WORD", "内容命中敏感词，仅记录日志", value, sensitiveWords, matches.size());
                log.info("[敏感词检测] 发现敏感词: {}", sensitiveWords);
                return true;

            default:
                return false;
        }
    }

    /**
     * 正则检测联系方式
     */
    private List<RegexMatch> checkRegexPatterns(String text, ConstraintValidatorContext context) {
        List<RegexMatch> matches = new ArrayList<>();

        // 手机号
        Matcher phoneMatcher = PHONE_PATTERN.matcher(text);
        while (phoneMatcher.find()) {
            matches.add(new RegexMatch(phoneMatcher.start(), phoneMatcher.end(),
                    "PHONE", phoneMatcher.group()));
        }

        // 身份证号
        Matcher idCardMatcher = ID_CARD_CANDIDATE_PATTERN.matcher(text);
        while (idCardMatcher.find()) {
            String idCard = idCardMatcher.group();
            if (idCardValidationService.isValid(idCard, context)) {
                matches.add(new RegexMatch(idCardMatcher.start(), idCardMatcher.end(),
                        "ID_CARD", idCard));
            }
        }

        // 银行卡号（16-19位）
        Matcher bankCardMatcher = BANK_CARD_PATTERN.matcher(text);
        while (bankCardMatcher.find()) {
            String card = bankCardMatcher.group();
            if (bankCardValidationService.isValid(card, context)) {
                matches.add(new RegexMatch(bankCardMatcher.start(), bankCardMatcher.end(),
                        "BANK_CARD", card));
            }
        }

        // 邮箱
        Matcher emailMatcher = EMAIL_PATTERN.matcher(text);
        while (emailMatcher.find()) {
            matches.add(new RegexMatch(emailMatcher.start(), emailMatcher.end(),
                    "EMAIL", emailMatcher.group()));
        }

        return matches;
    }

    private List<SensitiveWordManager.MatchResult> filterIgnoreWords(
            List<SensitiveWordManager.MatchResult> matches) {

        if (ignoreWords.isEmpty()) return matches;

        return matches.stream()
                .filter(m -> !ignoreWords.contains(m.getWord()))
                .collect(Collectors.toList());
    }

    private void setMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

    private String normalizeCategoryCode(String categoryCode) {
        if (categoryCode == null) {
            return null;
        }
        String normalized = categoryCode.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private boolean handleRegexViolation(List<RegexMatch> matches,
                                         ConstraintValidatorContext context) {
        String leakInfo = matches.stream()
                .map(m -> m.getType() + ":" + StringUtils.mask(m.getContent(), 3, 3))
                .collect(Collectors.joining(", "));

        switch (annotation.strategy()) {
            case REJECT:
                record("REGEX_PATTERN", "内容包含敏感信息泄露: " + leakInfo, leakInfo, leakInfo, matches.size());
                setMessage(context, "内容包含敏感信息泄露: " + leakInfo);
                return false;

            case MASK:
            case LOG:
                record("REGEX_PATTERN", "内容命中敏感信息泄露规则", leakInfo, leakInfo, matches.size());
                log.warn("[敏感信息泄露] 类型: {}, 内容: {}",
                        matches.get(0).getType(),
                        StringUtils.mask(matches.get(0).getContent(), 3, 3));
                return true;

            case REVIEW:
                record("REGEX_PATTERN", "内容命中敏感信息泄露规则，已标记待审核", leakInfo, leakInfo, matches.size());
                log.warn("[待审核-信息泄露] {}", leakInfo);
                return true;

            default:
                return false;
        }
    }


    private boolean handleAiViolation(AiContentChecker.AiCheckResult aiResult,
                                      ConstraintValidatorContext context) {
        String msg = String.format("内容疑似违规 [%s]，置信度: %.2f",
                aiResult.getLabel(), aiResult.getConfidence());

        switch (annotation.strategy()) {
            case REJECT:
                record("SEMANTIC_ANALYSIS", msg, aiResult.getLabel(), msg, 1);
                setMessage(context, msg);
                return false;

            case REVIEW:
                record("SEMANTIC_ANALYSIS", msg, aiResult.getLabel(), msg, 1);
                log.warn("[AI审核-待人工复核] {}", msg);
                return true;

            case LOG:
                record("SEMANTIC_ANALYSIS", msg, aiResult.getLabel(), msg, 1);
                log.info("[AI检测] {}", msg);
                return true;

            default:
                return false;
        }
    }

    private void record(String detectorType,
                        String message,
                        String content,
                        String matchedDetail,
                        int hitCount) {
        if (recorder == null) {
            return;
        }
        ContentValidationRecord record = ContentValidationRecord.builder()
                .detectorType(detectorType)
                .strategy(annotation.strategy().name())
                .message(message)
                .contentPreview(StringUtils.abbreviate(content, 200))
                .matchedDetail(matchedDetail)
                .hitCount(hitCount)
                .occurredAt(LocalDateTime.now())
                .build();
        recorder.record(record);
    }
}
