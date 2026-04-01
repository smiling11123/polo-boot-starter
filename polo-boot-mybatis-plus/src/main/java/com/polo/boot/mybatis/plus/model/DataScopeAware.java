package com.polo.boot.mybatis.plus.model;

/**
 * 数据权限字段兼容标记接口。
 * 推荐优先在实体字段上使用 {@code @AutoFillField(type = AutoFillType.DEPT_ID)}，
 * 实现该接口时仍会按默认字段名 deptId 自动填充。
 */
public interface DataScopeAware {
}
