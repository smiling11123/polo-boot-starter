package com.polo.boot.security.model;

/**
 * 兼容旧用法的租户能力接口。
 * 推荐优先在字段上使用 {@code @SecurityAttributeField(type = SecurityAttributeType.TENANT_ID)}。
 */
public interface TenantAwarePrincipal {
    Long getTenantId();
}
