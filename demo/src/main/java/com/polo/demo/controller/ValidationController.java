package com.polo.demo.controller;

import com.polo.boot.api.doc.annotation.ApiOperation;
import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.core.model.Result;
import com.polo.boot.security.annotation.RequireRole;
import com.polo.boot.validation.model.DynamicWordEntry;
import com.polo.boot.validation.service.SensitiveWordManager;
import com.polo.boot.validation.annotation.*;
import com.polo.boot.web.annotation.OperationLog;
import com.polo.boot.web.annotation.OperationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/validate")
@Validated
public class ValidationController {
    private final SensitiveWordManager sensitiveWordManager;

    public ValidationController(SensitiveWordManager sensitiveWordManager) {
        this.sensitiveWordManager = sensitiveWordManager;
    }

    // ─────────────────── 全量 DTO 校验 ───────────────────

    @PostMapping("/full")
    @ApiOperation(value = "全量校验", description = "演示所有自定义校验注解的 DTO 级校验")
    @OperationLog(module = "校验中心", type = OperationType.OTHER, desc = "全量校验测试")
    public Result<ValidationDTO> validateFull(@Valid @RequestBody ValidationDTO dto) {
        return Result.success(dto);
    }

    // ─────────────────── 单字段参数级校验 ───────────────────

    @GetMapping("/phone")
    @ApiOperation(value = "手机号校验", description = "参数级 @Phone 注解演示")
    @OperationLog(module = "校验中心", type = OperationType.OTHER, desc = "手机号校验")
    public Result<String> validatePhone(@Phone @RequestParam String phone) {
        return Result.success("手机号格式正确: " + phone);
    }

    @GetMapping("/email")
    @ApiOperation(value = "邮箱校验", description = "参数级 @Email 注解演示")
    @OperationLog(module = "校验中心", type = OperationType.OTHER, desc = "邮箱校验")
    public Result<String> validateEmail(@Email @RequestParam String email) {
        return Result.success("邮箱格式正确: " + email);
    }

    @GetMapping("/id-card")
    @ApiOperation(value = "身份证校验", description = "参数级 @IdCard 注解演示")
    @OperationLog(module = "校验中心", type = OperationType.OTHER, desc = "身份证校验")
    public Result<String> validateIdCard(@IdCard @RequestParam String idCard) {
        return Result.success("身份证格式正确: " + idCard);
    }

    @GetMapping("/bank-card")
    @ApiOperation(value = "银行卡号校验", description = "参数级 @BankCard 注解演示")
    @OperationLog(module = "校验中心", type = OperationType.OTHER, desc = "银行卡号校验")
    public Result<String> validateBankCard(@BankCard @RequestParam String bankCard) {
        return Result.success("银行卡号格式正确: " + bankCard);
    }

    @GetMapping("/car-no")
    @ApiOperation(value = "车牌号校验", description = "参数级 @CarNo 注解演示")
    @OperationLog(module = "校验中心", type = OperationType.OTHER, desc = "车牌号校验")
    public Result<String> validateCarNo(@CarNo @RequestParam String carNo) {
        return Result.success("车牌号格式正确: " + carNo);
    }

    @GetMapping("/credit-code")
    @ApiOperation(value = "统一社会信用代码校验", description = "参数级 @CreditCode 注解演示")
    @OperationLog(module = "校验中心", type = OperationType.OTHER, desc = "统一社会信用代码校验")
    public Result<String> validateCreditCode(@CreditCode @RequestParam String creditCode) {
        return Result.success("统一社会信用代码格式正确: " + creditCode);
    }

    @GetMapping("/ip")
    @ApiOperation(value = "IP 地址校验", description = "参数级 @IPAddress 注解演示")
    @OperationLog(module = "校验中心", type = OperationType.OTHER, desc = "IP 地址校验")
    public Result<String> validateIp(@IPAddress @RequestParam String ip) {
        return Result.success("IP 地址格式正确: " + ip);
    }

    @PostMapping("/password")
    @ApiOperation(value = "密码强度校验", description = "参数级 @Password 注解演示，要求至少8位，含数字和小写字母")
    @OperationLog(module = "校验中心", type = OperationType.OTHER, desc = "密码强度校验")
    public Result<String> validatePassword(@Password @RequestParam String password) {
        return Result.success("密码强度合格");
    }

    @PostMapping("/content")
    @ApiOperation(value = "内容安全校验", description = "参数级 @InputContent 注解演示，检测敏感词")
    @OperationLog(module = "校验中心", type = OperationType.OTHER, desc = "内容安全校验")
    public Result<String> validateContent(
            @InputContent(types = {InputContent.CheckType.SENSITIVE_WORD, InputContent.CheckType.REGEX_PATTERN})
            @RequestParam String content) {
        return Result.success("内容安全: " + content);
    }

    @GetMapping("/dynamic-words")
    @RequireRole("admin")
    @ApiOperation(value = "查看动态词库", description = "读取 Redis 动态词库中的敏感词条目，返回当前 key、总数和词条列表")
    @OperationLog(module = "校验中心", type = OperationType.QUERY, desc = "查看动态词库", logResult = true)
    public Result<Map<String, Object>> listDynamicWords() {
        ensureDynamicWordManagementAvailable();
        List<DynamicWordEntry> entries = sensitiveWordManager.listDynamicWords();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dynamicEnabled", true);
        result.put("databaseEnabled", sensitiveWordManager.isDatabaseWordManagementAvailable());
        result.put("providerCount", sensitiveWordManager.getRegisteredProviderCount());
        result.put("redisKey", sensitiveWordManager.getDynamicWordRedisKey());
        result.put("count", entries.size());
        result.put("entries", entries);
        return Result.success(result);
    }

    @PostMapping("/dynamic-words")
    @RequireRole("admin")
    @ApiOperation(value = "新增动态敏感词", description = "向 Redis 动态词库新增一条敏感词，并立即加入当前应用内存词树")
    @OperationLog(module = "校验中心", type = OperationType.CREATE, desc = "新增动态敏感词", logResult = true)
    public Result<DynamicWordEntry> addDynamicWord(@Valid @RequestBody DynamicWordRequest request) {
        ensureDynamicWordManagementAvailable();
        return Result.success(sensitiveWordManager.addDynamicWord(
                request.getWord(),
                parseCategory(request.getCategory()),
                request.getLevel()
        ));
    }

    @DeleteMapping("/dynamic-words")
    @RequireRole("admin")
    @ApiOperation(value = "删除动态敏感词", description = "从 Redis 动态词库删除指定敏感词，并立即重建内存词树")
    @OperationLog(module = "校验中心", type = OperationType.DELETE, desc = "删除动态敏感词", logResult = true)
    public Result<Map<String, Object>> removeDynamicWord(@RequestParam String word) {
        ensureDynamicWordManagementAvailable();
        boolean removed = sensitiveWordManager.removeDynamicWord(word);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("word", word);
        result.put("removed", removed);
        result.put("redisKey", sensitiveWordManager.getDynamicWordRedisKey());
        return Result.success(result);
    }

    @PostMapping("/dynamic-words/refresh")
    @RequireRole("admin")
    @ApiOperation(value = "手动刷新动态词库", description = "立即重载内置词库和 Redis 动态词库，不必等待定时任务")
    @OperationLog(module = "校验中心", type = OperationType.OTHER, desc = "手动刷新动态词库", logResult = true)
    public Result<Map<String, Object>> refreshDynamicWords() {
        ensureDynamicWordManagementAvailable();
        int totalCount = sensitiveWordManager.refreshNow();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("redisKey", sensitiveWordManager.getDynamicWordRedisKey());
        result.put("totalWordCount", totalCount);
        result.put("dynamicWordCount", sensitiveWordManager.listDynamicWords().size());
        result.put("databaseEnabled", sensitiveWordManager.isDatabaseWordManagementAvailable());
        result.put("providerCount", sensitiveWordManager.getRegisteredProviderCount());
        return Result.success(result);
    }

    private void ensureDynamicWordManagementAvailable() {
        if (sensitiveWordManager.isDynamicWordManagementAvailable()) {
            return;
        }
        throw new BizException(ErrorCode.FORBIDDEN.getCode(), "当前未开启 Redis 动态词库或 Redis 不可用");
    }

    private String parseCategory(String rawCategory) {
        String normalizedCategory = rawCategory == null ? InputContent.CategoryCodes.ADVERTISING : rawCategory.trim();
        if (normalizedCategory.isEmpty()) {
            normalizedCategory = InputContent.CategoryCodes.ADVERTISING;
        }
        return normalizedCategory.toUpperCase(java.util.Locale.ROOT);
    }

    // ─────────────────── 全量校验 DTO ───────────────────

    @Data
    public static class ValidationDTO {
        @NotBlank(message = "姓名不能为空")
        private String name;

        @Phone
        private String phone;

        @Email
        private String email;

        @IdCard
        private String idCard;

        @Password(min = 8, requireDigit = true, requireLower = true, requireUpper = false)
        private String password;

        @BankCard
        private String bankCard;

        @CarNo
        private String carNo;

        @CreditCode
        private String creditCode;

        @IPAddress(version = {IPAddress.IPVersion.V4})
        private String ip;
    }

    @Data
    public static class DynamicWordRequest {
        @NotBlank(message = "敏感词不能为空")
        private String word;

        private String category = InputContent.CategoryCodes.ADVERTISING;

        private int level = 3;
    }
}
