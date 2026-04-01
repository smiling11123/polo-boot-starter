package com.polo.boot.mybatis.plus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {
    /** 权限范围 */
    DataScopeType type() default DataScopeType.DEPT_AND_CHILD;

    /** 部门字段名（用于 SQL 拼接） */
    String deptColumn() default "dept_id";

    /** 创建者字段名 */
    String userColumn() default "create_by";

    /** 自定义 SQL 条件（高级） */
    String customCondition() default "";
}

