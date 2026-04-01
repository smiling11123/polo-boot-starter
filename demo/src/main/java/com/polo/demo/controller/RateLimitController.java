package com.polo.demo.controller;

import com.polo.boot.api.doc.annotation.ApiOperation;
import com.polo.boot.core.model.Result;
import com.polo.boot.security.annotation.PreventDuplicateSubmit;
import com.polo.boot.security.annotation.KeyStrategy;
import com.polo.boot.security.annotation.RateLimit;
import com.polo.boot.web.annotation.OperationLog;
import com.polo.boot.web.annotation.OperationType;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/rate-limit")
public class RateLimitController {

    // ═══════════════════════════════════════════════════
    //   @RateLimit 演示 — 不同维度 & 算法
    // ═══════════════════════════════════════════════════

    @GetMapping("/global")
    @RateLimit(type = RateLimit.LimitType.DEFAULT, rate = 5, capacity = 10,
            algorithm = RateLimit.Algorithm.TOKEN_BUCKET,
            message = "全局接口限流，请稍后再试")
    @ApiOperation(value = "全局限流", description = "演示 DEFAULT 维度 + 令牌桶算法，每秒 5 次，桶容量 10")
    @OperationLog(module = "限流中心", type = OperationType.QUERY, desc = "全局限流测试")
    public Result<Map<String, Object>> globalRateLimit() {
        return Result.success(Map.of(
                "type", "GLOBAL / TOKEN_BUCKET",
                "time", LocalDateTime.now().toString(),
                "message", "请求成功（全局限流）"
        ));
    }

    @GetMapping("/user")
    @RateLimit(type = RateLimit.LimitType.USER, rate = 3, capacity = 5,
            algorithm = RateLimit.Algorithm.SLIDING_WINDOW,
            message = "用户级限流，请稍后再试")
    @ApiOperation(value = "用户级限流", description = "演示 USER 维度 + 滑动窗口算法，每秒 3 次，窗口大小 5")
    @OperationLog(module = "限流中心", type = OperationType.QUERY, desc = "用户级限流测试")
    public Result<Map<String, Object>> userRateLimit() {
        return Result.success(Map.of(
                "type", "USER / SLIDING_WINDOW",
                "time", LocalDateTime.now().toString(),
                "message", "请求成功（用户级限流）"
        ));
    }

    @GetMapping("/ip")
    @RateLimit(type = RateLimit.LimitType.IP, rate = 2, capacity = 3,
            algorithm = RateLimit.Algorithm.FIXED_WINDOW,
            message = "IP 级限流，请稍后再试")
    @ApiOperation(value = "IP 级限流", description = "演示 IP 维度 + 固定窗口算法，每秒 2 次，窗口大小 3")
    @OperationLog(module = "限流中心", type = OperationType.QUERY, desc = "IP 级限流测试")
    public Result<Map<String, Object>> ipRateLimit() {
        return Result.success(Map.of(
                "type", "IP / FIXED_WINDOW",
                "time", LocalDateTime.now().toString(),
                "message", "请求成功（IP 级限流）"
        ));
    }

    @GetMapping("/custom")
    @RateLimit(type = RateLimit.LimitType.CUSTOM, key = "'demo:custom:' + #key",
            rate = 5, capacity = 10, message = "自定义 Key 限流，请稍后再试")
    @ApiOperation(value = "自定义 Key 限流", description = "演示 CUSTOM 维度，使用 SpEL 表达式构建限流 key")
    @OperationLog(module = "限流中心", type = OperationType.QUERY, desc = "自定义 Key 限流测试")
    public Result<Map<String, Object>> customRateLimit(@RequestParam String key) {
        return Result.success(Map.of(
                "type", "CUSTOM / TOKEN_BUCKET",
                "customKey", key,
                "time", LocalDateTime.now().toString(),
                "message", "请求成功（自定义 Key 限流）"
        ));
    }

    // ═══════════════════════════════════════════════════
    //   @PreventDuplicateSubmit 演示
    // ═══════════════════════════════════════════════════

    @PostMapping("/submit")
    @PreventDuplicateSubmit(interval = 5, message = "请勿重复提交，5 秒内只能提交一次",
            strategy = KeyStrategy.USER_AND_METHOD)
    @ApiOperation(value = "防重复提交(用户+方法)", description = "演示 USER_AND_METHOD 策略，5 秒内同一用户不可重复提交")
    @OperationLog(module = "限流中心", type = OperationType.CREATE, desc = "防重复提交测试")
    public Result<Map<String, Object>> preventDuplicateSubmit(@RequestBody SubmitRequest request) {
        return Result.success(Map.of(
                "strategy", "USER_AND_METHOD",
                "interval", "5s",
                "time", LocalDateTime.now().toString(),
                "data", request.getData(),
                "message", "提交成功"
        ));
    }

    @PostMapping("/submit-custom")
    @PreventDuplicateSubmit(interval = 10, message = "该订单号 10 秒内已提交",
            strategy = KeyStrategy.CUSTOM, keyExpression = "#p0.orderNo")
    @ApiOperation(value = "防重复提交(自定义 Key)", description = "演示 CUSTOM 策略，按 orderNo 防重")
    @OperationLog(module = "限流中心", type = OperationType.CREATE, desc = "'防重复提交[' + #p0.orderNo + ']'")
    public Result<Map<String, Object>> preventDuplicateSubmitCustom(@RequestBody OrderSubmitRequest request) {
        return Result.success(Map.of(
                "strategy", "CUSTOM",
                "keyExpression", "#p0.orderNo",
                "interval", "10s",
                "orderNo", request.getOrderNo(),
                "time", LocalDateTime.now().toString(),
                "message", "订单提交成功"
        ));
    }

    @PostMapping("/submit-immediate")
    @PreventDuplicateSubmit(interval = 3, immediateRelease = true,
            message = "请勿重复提交（执行完立即释放锁）")
    @ApiOperation(value = "防重复提交(立即释放)", description = "演示 immediateRelease=true，方法执行完立即释放锁")
    @OperationLog(module = "限流中心", type = OperationType.CREATE, desc = "防重复提交(立即释放)测试")
    public Result<Map<String, Object>> preventDuplicateImmediate(@RequestBody SubmitRequest request) {
        return Result.success(Map.of(
                "strategy", "USER_AND_METHOD",
                "immediateRelease", true,
                "time", LocalDateTime.now().toString(),
                "data", request.getData(),
                "message", "提交成功（锁已立即释放）"
        ));
    }

    // ─────────────────── 请求 DTO ───────────────────

    @Data
    public static class SubmitRequest {
        private String data;
    }

    @Data
    public static class OrderSubmitRequest {
        private String orderNo;
        private String data;
    }
}
