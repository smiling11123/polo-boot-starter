package com.polo.boot.security.model;

/**
 * 兼容旧用法的部门能力接口。
 * 推荐优先在字段上使用 {@code @SecurityAttributeField(type = SecurityAttributeType.DEPT_ID)}。
 */
public interface DeptAwarePrincipal {
    Long getDeptId();
}
