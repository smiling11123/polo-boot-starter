package com.polo.boot.mybatis.plus.service;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.polo.boot.core.context.SecurityContextFacade;
import com.polo.boot.mybatis.plus.config.TenantProperties;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomTenantLineHandler implements TenantLineHandler {
    private static final long NOOP_TENANT_ID = -1L;

    private final TenantProperties tenantProperties;
    private final SecurityContextFacade securityContextFacade;
    private final Set<String> ignoredTables;

    public CustomTenantLineHandler(TenantProperties tenantProperties, SecurityContextFacade securityContextFacade) {
        this.tenantProperties = tenantProperties;
        this.securityContextFacade = securityContextFacade;
        this.ignoredTables = tenantProperties.getIgnoreTables().stream()
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Expression getTenantId() {
        Long tenantId = securityContextFacade.getTenantId();
        return new LongValue(tenantId != null ? tenantId : NOOP_TENANT_ID);
    }

    @Override
    public String getTenantIdColumn() {
        return tenantProperties.getTenantIdColumn();
    }

    @Override
    public boolean ignoreTable(String tableName) {
        Long tenantId = securityContextFacade.getTenantId();
        if (tenantId == null && tenantProperties.isIgnoreIfMissing()) {
            return true;
        }
        return ignoredTables.contains(normalize(tableName));
    }

    private String normalize(String tableName) {
        return tableName == null ? "" : tableName.trim().toLowerCase(Locale.ROOT);
    }
}
