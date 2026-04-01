package com.polo.boot.mybatis.plus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@ConfigurationProperties(prefix = "polo.mybatis-plus.tenant")
public class TenantProperties {
    /**
     * 是否启用租户隔离。
     */
    private boolean enabled = false;

    /**
     * 数据表中的租户字段名。
     */
    private String tenantIdColumn = "tenant_id";

    /**
     * 当上下文中缺少 tenantId 时是否忽略租户条件。
     */
    private boolean ignoreIfMissing = true;

    /**
     * 不参与租户隔离的表名列表。
     */
    private Set<String> ignoreTables = new LinkedHashSet<>();
}
