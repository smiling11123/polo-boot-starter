package com.polo.boot.web.handler;

import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.core.model.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<?> handleBizException(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), extractValidationMessage(e.getBindingResult()));
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), extractValidationMessage(e.getBindingResult()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(ErrorCode.PARAM_ERROR.getMessage());
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), "缺少必须的请求参数：" + e.getParameterName());
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public Result<?> handleMethodArgumentTypeMismatchException(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), "参数 [" + e.getName() + "] 格式不符合要求，请检查传参");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), "请求的数据格式有误或不可读（可能是 JSON 拼写错误），请检查");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), "上传文件超过服务器允许的大小限制，请调整文件大小后重试");
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsableException(AsyncRequestNotUsableException e) {
        log.debug("[GlobalExceptionHandler] 客户端已中断响应输出，忽略异常: {}", e.getMessage());
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public Result<?> handleHttpRequestMethodNotSupportedException(org.springframework.web.HttpRequestMethodNotSupportedException e) {
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), "不支持此请求方式 (" + e.getMethod() + ")，请核对接口文档");
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public Result<?> handleHttpMediaTypeNotSupportedException(org.springframework.web.HttpMediaTypeNotSupportedException e) {
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), "不支持的数据内容类型 (Content-Type)，请使用正确的格式提交");
    }

    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    public Result<?> handleNoHandlerFoundException(org.springframework.web.servlet.NoHandlerFoundException e) {
        return Result.fail(404, "访问的接口路径不存在：" + e.getRequestURL());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("[GlobalExceptionHandler] 拦截到未预知的系统异常 (类型: {})", e.getClass().getSimpleName(), e);
        String msg = "系统遇到了一些故障，请稍后再试";
        if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            msg = e.getMessage() != null ? e.getMessage() : "请求状态或参数不合法，请检查";
        }
        return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), msg);
    }

    private String extractValidationMessage(BindingResult bindingResult) {
        FieldError fieldError = bindingResult.getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null && !fieldError.getDefaultMessage().isBlank()) {
            return fieldError.getDefaultMessage();
        }
        return ErrorCode.PARAM_ERROR.getMessage();
    }
}
