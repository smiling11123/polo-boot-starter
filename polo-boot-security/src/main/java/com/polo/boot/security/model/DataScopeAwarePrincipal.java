package com.polo.boot.security.model;

/**
 * 兼容旧用法的数据权限能力接口。
 * 推荐优先在字段上使用 {@code @SecurityAttributeField(type = SecurityAttributeType.DATA_SCOPE)}。
 */
public interface DataScopeAwarePrincipal {
    String getDataScope();
}
