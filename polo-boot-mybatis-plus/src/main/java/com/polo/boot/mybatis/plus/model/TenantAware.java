package com.polo.boot.mybatis.plus.model;

/**
 * 多租户字段兼容标记接口。
 * 推荐优先在实体字段上使用 {@code @AutoFillField(type = AutoFillType.TENANT_ID)}，
 * 实现该接口时仍会按默认字段名 tenantId 自动填充。
 */
public interface TenantAware {
}
