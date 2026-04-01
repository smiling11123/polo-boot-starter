package com.polo.boot.mybatis.plus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "polo.mybatis-plus.data-scope")
public class DataScopeProperties {
    /**
     * 是否启用数据权限拦截。
     */
    private boolean enabled = false;

    /**
     * 是否允许管理员绕过数据权限限制。
     */
    private boolean adminBypass = true;

    /**
     * 当上下文缺少部门、审计人等关键属性时是否直接拒绝查询。
     */
    private boolean denyWhenAttributeMissing = true;
}
