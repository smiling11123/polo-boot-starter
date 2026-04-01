package com.polo.demo.controller;

import com.polo.boot.api.doc.annotation.ApiOperation;
import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.core.model.Result;
import com.polo.boot.web.annotation.OperationLog;
import com.polo.boot.web.annotation.OperationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/exception")
@Validated
public class ExceptionDemoController {

    // ─────────────── BizException 演示 ───────────────

    @GetMapping("/biz")
    @ApiOperation(value = "业务异常", description = "触发 BizException，演示自定义错误码")
    @OperationLog(module = "异常中心", type = OperationType.OTHER, desc = "业务异常演示")
    public Result<Void> bizException() {
        throw new BizException(ErrorCode.DATA_NOT_FOUND);
    }

    @GetMapping("/biz-format")
    @ApiOperation(value = "格式化业务异常", description = "使用 ErrorCode.format 输出带占位符的错误消息")
    @OperationLog(module = "异常中心", type = OperationType.OTHER, desc = "格式化业务异常演示")
    public Result<Void> bizExceptionFormat() {
        throw ErrorCode.USER_ALREADY_EXISTS.exception("demo_user");
    }

    @GetMapping("/biz-custom")
    @ApiOperation(value = "自定义业务异常", description = "使用自定义 code+message 构造 BizException")
    @OperationLog(module = "异常中心", type = OperationType.OTHER, desc = "自定义业务异常演示")
    public Result<Void> bizExceptionCustom() {
        throw new BizException(99999, "这是一个自定义业务异常");
    }

    // ─────────────── MethodArgumentNotValidException 演示 ───────────────

    @PostMapping("/validation")
    @ApiOperation(value = "参数校验异常(@RequestBody)", description = "触发 MethodArgumentNotValidException")
    @OperationLog(module = "异常中心", type = OperationType.OTHER, desc = "参数校验异常演示")
    public Result<ExceptionDTO> validationException(@Valid @RequestBody ExceptionDTO dto) {
        return Result.success(dto);
    }

    // ─────────────── ConstraintViolationException 演示 ───────────────

    @GetMapping("/constraint")
    @ApiOperation(value = "约束违反异常(@RequestParam)", description = "触发 ConstraintViolationException")
    @OperationLog(module = "异常中心", type = OperationType.OTHER, desc = "约束违反异常演示")
    public Result<String> constraintViolation(
            @NotBlank(message = "名称不能为空") @RequestParam String name,
            @Min(value = 1, message = "年龄必须大于 0") @RequestParam int age) {
        return Result.success("name=" + name + ", age=" + age);
    }

    // ─────────────── MissingServletRequestParameterException 演示 ───────────────

    @GetMapping("/missing-param")
    @ApiOperation(value = "缺少参数异常", description = "触发 MissingServletRequestParameterException")
    @OperationLog(module = "异常中心", type = OperationType.OTHER, desc = "缺少参数异常演示")
    public Result<String> missingParam(@RequestParam String requiredParam) {
        return Result.success("收到参数: " + requiredParam);
    }

    // ─────────────── 未捕获异常演示 ───────────────

    @GetMapping("/system")
    @ApiOperation(value = "系统异常", description = "触发未捕获异常，演示全局兜底处理")
    @OperationLog(module = "异常中心", type = OperationType.OTHER, desc = "系统异常演示")
    public Result<Void> systemException() {
        throw new RuntimeException("模拟系统内部异常");
    }

    // ─────────────── ErrorCode 枚举全景演示 ───────────────

    @GetMapping("/error-code")
    @ApiOperation(value = "ErrorCode 枚举演示", description = "根据 category 触发不同分类的 ErrorCode")
    @OperationLog(module = "异常中心", type = OperationType.OTHER, desc = "'ErrorCode演示[' + #category + ']'")
    public Result<Void> errorCodeDemo(@RequestParam(defaultValue = "auth") String category) {
        switch (category) {
            case "auth" -> throw new BizException(ErrorCode.UNAUTHORIZED);
            case "forbidden" -> throw new BizException(ErrorCode.FORBIDDEN);
            case "locked" -> throw new BizException(ErrorCode.ACCOUNT_LOCKED);
            case "param" -> throw new BizException(ErrorCode.PARAM_ERROR);
            case "rate" -> throw new BizException(ErrorCode.RATE_LIMIT);
            case "repeat" -> throw new BizException(ErrorCode.REPEAT_SUBMIT);
            case "not-found" -> throw new BizException(ErrorCode.DATA_NOT_FOUND);
            case "conflict" -> throw new BizException(ErrorCode.CONCURRENT_CONFLICT);
            case "storage" -> throw new BizException(ErrorCode.STORAGE_ERROR);
            case "cache" -> throw new BizException(ErrorCode.CACHE_ERROR);
            case "third-party" -> throw new BizException(ErrorCode.THIRD_PARTY_ERROR);
            default -> throw new BizException(ErrorCode.SYSTEM_ERROR);
        }
    }

    // ─────────────── DTO ───────────────

    @Data
    public static class ExceptionDTO {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @Min(value = 0, message = "年龄不能为负数")
        private int age;
    }
}
