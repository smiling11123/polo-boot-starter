package com.polo.boot.mybatis.plus.model;

/**
 * 审计字段兼容标记接口。
 * 推荐优先在实体字段上使用 {@code @AutoFillField}，实现该接口时仍会按默认字段名
 * 自动填充 createTime/createBy/updateTime/updateBy。
 */
public interface Auditable {
}
