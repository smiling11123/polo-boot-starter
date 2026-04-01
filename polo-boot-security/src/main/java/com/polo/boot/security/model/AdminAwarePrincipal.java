package com.polo.boot.security.model;

/**
 * 兼容旧用法的管理员标记接口。
 * 推荐优先在字段上使用 {@code @SecurityAttributeField(type = SecurityAttributeType.IS_ADMIN)}。
 */
public interface AdminAwarePrincipal {
    Boolean getAdminFlag();
}
