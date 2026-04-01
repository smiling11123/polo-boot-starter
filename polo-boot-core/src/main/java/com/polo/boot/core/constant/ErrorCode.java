package com.polo.boot.core.constant;

import com.polo.boot.core.exception.BizException;
import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 统一错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ═══════════════════════════════════════════════════
    // 1xxxx 系统级错误（基础设施）
    // ═══════════════════════════════════════════════════

    SUCCESS(200, "success"),
    SYSTEM_ERROR(10000, "系统繁忙，请稍后再试"),
    SYSTEM_MAINTENANCE(10001, "系统维护中，请稍后访问"),

    // 数据库
    DB_ERROR(10100, "数据库操作失败"),
    DB_CONNECTION_TIMEOUT(10101, "数据库连接超时"),
    DB_QUERY_TIMEOUT(10102, "数据库查询超时"),

    // Redis
    CACHE_ERROR(10200, "缓存操作失败"),
    CACHE_CONNECTION_ERROR(10201, "缓存服务不可用"),

    // 消息队列
    MQ_ERROR(10300, "消息发送失败"),
    MQ_CONSUME_ERROR(10301, "消息消费异常"),

    // 文件存储
    STORAGE_ERROR(10400, "文件存储失败"),
    STORAGE_UPLOAD_ERROR(10401, "文件上传失败"),
    STORAGE_DOWNLOAD_ERROR(10402, "文件下载失败"),
    STORAGE_DELETE_ERROR(10403, "文件删除失败"),

    // ═══════════════════════════════════════════════════
    // 2xxxx 认证授权错误（安全）
    // ═══════════════════════════════════════════════════

    UNAUTHORIZED(20000, "请先登录"),
    TOKEN_MISSING(20001, "缺少访问令牌"),
    TOKEN_EXPIRED(20002, "登录已过期，请重新登录"),
    TOKEN_INVALID(20003, "无效的令牌或签名"),
    TOKEN_REFRESH_FAILED(20004, "令牌刷新失败"),

    FORBIDDEN(20100, "无权限访问该资源"),
    PERMISSION_DENIED(20101, "缺少必要权限：%s"),
    ROLE_DENIED(20102, "需要角色：%s"),

    // 账号安全
    ACCOUNT_LOCKED(20200, "账号已被锁定，请联系客服"),
    ACCOUNT_DISABLED(20201, "账号已禁用"),
    PASSWORD_EXPIRED(20202, "密码已过期，请修改密码"),
    LOGIN_FAILED(20203, "用户名或密码错误"),

    // ═══════════════════════════════════════════════════
    // 3xxxx 业务通用错误（输入、状态、数据）
    // ═══════════════════════════════════════════════════

    PARAM_ERROR(30000, "请求参数错误"),
    PARAM_MISSING(30001, "缺少必要参数：%s"),
    PARAM_TYPE_ERROR(30002, "参数类型错误：%s"),
    PARAM_FORMAT_ERROR(30003, "参数格式错误：%s"),

    // 数据校验
    VALIDATION_ERROR(30100, "数据校验失败"),
    VALIDATION_NOT_NULL(30101, "%s不能为空"),
    VALIDATION_LENGTH(30102, "%s长度必须在%d到%d之间"),
    VALIDATION_PATTERN(30103, "%s格式不正确"),

    // 数据状态
    DATA_NOT_FOUND(30200, "数据不存在"),
    DATA_ALREADY_EXISTS(30201, "数据已存在：%s"),
    DATA_EXPIRED(30202, "数据已过期"),
    DATA_STATUS_ERROR(30203, "数据状态不正确，当前：%s，期望：%s"),

    // 操作限制
    RATE_LIMIT(30300, "操作过于频繁，请过几秒后再试"),
    REPEAT_SUBMIT(30301, "请勿重复提交"),
    CONCURRENT_CONFLICT(30302, "数据已被他人修改，请刷新后重试"),

    // ═══════════════════════════════════════════════════
    // 4xxxx 具体业务错误（按模块划分）
    // ═══════════════════════════════════════════════════

    // 41xxx 用户模块
    USER_NOT_FOUND(41000, "用户不存在"),
    USER_ALREADY_EXISTS(41001, "用户已存在：%s"),
    USER_PASSWORD_ERROR(41002, "密码错误"),
    USER_PHONE_EXISTS(41003, "手机号已被注册"),
    USER_EMAIL_EXISTS(41004, "邮箱已被注册"),

    // 42xxx 订单模块
    ORDER_NOT_FOUND(42000, "订单不存在"),
    ORDER_CREATE_FAILED(42001, "订单创建失败：%s"),
    ORDER_STATUS_ERROR(42002, "订单状态不允许此操作，当前：%s"),
    ORDER_AMOUNT_ERROR(42003, "订单金额异常，应付：%s，实付：%s"),
    ORDER_TIMEOUT(42004, "订单已超时关闭"),
    ORDER_CANCEL_FAILED(42005, "订单取消失败：%s"),

    // 43xxx 支付模块
    PAY_FAILED(43000, "支付失败：%s"),
    PAY_AMOUNT_MISMATCH(43001, "支付金额不匹配"),
    PAY_CHANNEL_ERROR(43002, "支付渠道异常：%s"),
    PAY_TIMEOUT(43003, "支付超时，请查询订单状态"),
    PAY_REFUND_FAILED(43004, "退款失败：%s"),
    PAY_REFUND_AMOUNT_ERROR(43005, "退款金额超过订单金额"),

    // 44xxx 库存模块
    STOCK_INSUFFICIENT(44000, "库存不足，剩余：%d，需要：%d"),
    STOCK_DEDUCT_FAILED(44001, "库存扣减失败"),
    STOCK_FROZEN_FAILED(44002, "库存预占失败"),

    // 45xxx 商品模块
    PRODUCT_NOT_FOUND(45000, "商品不存在"),
    PRODUCT_OFF_SHELF(45001, "商品已下架"),
    PRODUCT_PRICE_CHANGED(45002, "商品价格已变更，请刷新"),

    // ═══════════════════════════════════════════════════
    // 5xxxx 第三方服务错误（外部依赖）
    // ═══════════════════════════════════════════════════

    // 通用
    THIRD_PARTY_ERROR(50000, "外部服务异常"),
    THIRD_PARTY_TIMEOUT(50001, "外部服务超时"),
    THIRD_PARTY_RATE_LIMIT(50002, "外部服务限流"),

    // 具体服务（按服务商划分）
    SMS_SEND_FAILED(50100, "短信发送失败：%s"),
    EMAIL_SEND_FAILED(50200, "邮件发送失败：%s"),
    OSS_UPLOAD_FAILED(50300, "云存储上传失败：%s"),

    // 微信支付
    WX_PAY_ERROR(51000, "微信支付失败：%s"),
    WX_PAY_SIGN_ERROR(51001, "微信支付签名验证失败"),

    // 支付宝
    ALI_PAY_ERROR(52000, "支付宝支付失败：%s");

    // ═══════════════════════════════════════════════════

    private final int code;
    private final String message;

    /**
     * 格式化消息（支持占位符）
     */
    public String format(Object... args) {
        return String.format(message, args);
    }

    /**
     * 创建异常对象
     */
    public BizException exception(Object... args) {
        return new BizException(this.code, this.format(args));
    }
}
