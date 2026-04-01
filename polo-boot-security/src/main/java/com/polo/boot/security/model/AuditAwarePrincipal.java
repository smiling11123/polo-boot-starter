package com.polo.boot.security.model;

/**
 * 兼容旧用法的审计能力接口。
 * 推荐优先在字段上使用 {@code @SecurityAttributeField(type = SecurityAttributeType.AUDIT_USER_ID)}。
 */
public interface AuditAwarePrincipal {
    Long getAuditUserId();
}
