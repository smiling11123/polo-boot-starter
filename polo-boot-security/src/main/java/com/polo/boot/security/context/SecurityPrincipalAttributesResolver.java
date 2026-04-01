package com.polo.boot.security.context;

import com.polo.boot.security.model.AdminAwarePrincipal;
import com.polo.boot.security.model.AuditAwarePrincipal;
import com.polo.boot.security.model.DataScopeAwarePrincipal;
import com.polo.boot.security.model.DeptAwarePrincipal;
import com.polo.boot.security.model.SecurityAttributeProvider;
import com.polo.boot.security.model.TenantAwarePrincipal;
import com.polo.boot.security.model.UserPrincipal;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SecurityPrincipalAttributesResolver {
    private SecurityPrincipalAttributesResolver() {
    }

    public static Map<String, Object> resolve(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return Map.of();
        }

        Map<String, Object> attributes = new LinkedHashMap<>();

        mergeIfAbsent(attributes, resolveInterfaceAttributes(userPrincipal));
        if (userPrincipal instanceof SecurityAttributeProvider securityAttributeProvider) {
            mergeIfAbsent(attributes, securityAttributeProvider.provideSecurityAttributes());
        }
        merge(attributes, SecurityAnnotatedAttributeResolver.resolve(userPrincipal));
        merge(attributes, userPrincipal.getAttributes());
        return attributes;
    }

    public static Object getAttribute(UserPrincipal userPrincipal, String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return resolve(userPrincipal).get(key);
    }

    private static void merge(Map<String, Object> target, Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        source.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                target.put(key, value);
            }
        });
    }

    private static void mergeIfAbsent(Map<String, Object> target, Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        source.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                target.putIfAbsent(key, value);
            }
        });
    }

    private static Map<String, Object> resolveInterfaceAttributes(UserPrincipal userPrincipal) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (userPrincipal instanceof TenantAwarePrincipal tenantAwarePrincipal && tenantAwarePrincipal.getTenantId() != null) {
            attributes.put(SecurityAttributeKeys.TENANT_ID, tenantAwarePrincipal.getTenantId());
        }
        if (userPrincipal instanceof DeptAwarePrincipal deptAwarePrincipal && deptAwarePrincipal.getDeptId() != null) {
            attributes.put(SecurityAttributeKeys.DEPT_ID, deptAwarePrincipal.getDeptId());
        }
        if (userPrincipal instanceof DataScopeAwarePrincipal dataScopeAwarePrincipal && dataScopeAwarePrincipal.getDataScope() != null) {
            attributes.put(SecurityAttributeKeys.DATA_SCOPE, dataScopeAwarePrincipal.getDataScope());
        }
        if (userPrincipal instanceof AdminAwarePrincipal adminAwarePrincipal && adminAwarePrincipal.getAdminFlag() != null) {
            attributes.put(SecurityAttributeKeys.IS_ADMIN, adminAwarePrincipal.getAdminFlag());
        }
        if (userPrincipal instanceof AuditAwarePrincipal auditAwarePrincipal && auditAwarePrincipal.getAuditUserId() != null) {
            attributes.put(SecurityAttributeKeys.AUDIT_USER_ID, auditAwarePrincipal.getAuditUserId());
        }
        return attributes;
    }
}
