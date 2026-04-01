package com.polo.boot.mybatis.plus.annotation;

public enum DataScopeType {
    ALL,                // 全部数据（超管）
    DEPT_ONLY,          // 本部门
    DEPT_AND_CHILD,     // 本部门及子部门（默认）
    SELF_ONLY,          // 仅本人创建
    CUSTOM              // 自定义规则
}
