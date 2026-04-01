package com.polo.boot.security.annotation;

/**
 * Key生成策略枚举
 */
public enum KeyStrategy {
    USER_AND_METHOD,    // 用户ID + 方法签名（同用户同方法防重）
    USER_AND_URI,       // 用户ID + 请求URI（同用户同接口防重）
    METHOD_ONLY,        // 仅方法签名（所有用户共享，慎用）
    CUSTOM              // 自定义SpEL表达式（最灵活）
}
