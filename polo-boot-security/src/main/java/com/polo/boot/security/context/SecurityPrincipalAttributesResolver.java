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

    public static Map<String, Object> resolve(Object principalSource) {
        if (principalSource == null) {
            return Map.of();
        }

        Map<String, Object> attributes = new LinkedHashMap<>();

        mergeIfAbsent(attributes, resolveInterfaceAttributes(principalSource));
        if (principalSource instanceof SecurityAttributeProvider securityAttributeProvider) {
            mergeIfAbsent(attributes, securityAttributeProvider.provideSecurityAttributes());
        }
        merge(attributes, SecurityAnnotatedAttributeResolver.resolve(principalSource));
        if (principalSource instanceof UserPrincipal userPrincipal) {
            merge(attributes, userPrincipal.getAttributes());
        }
        return attributes;
    }

    public static Map<String, Object> resolve(UserPrincipal userPrincipal) {
        return resolve((Object) userPrincipal);
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

    private static Map<String, Object> resolveInterfaceAttributes(Object principalSource) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (principalSource instanceof TenantAwarePrincipal tenantAwarePrincipal && tenantAwarePrincipal.getTenantId() != null) {
            attributes.put(SecurityAttributeKeys.TENANT_ID, tenantAwarePrincipal.getTenantId());
        }
        if (principalSource instanceof DeptAwarePrincipal deptAwarePrincipal && deptAwarePrincipal.getDeptId() != null) {
            attributes.put(SecurityAttributeKeys.DEPT_ID, deptAwarePrincipal.getDeptId());
        }
        if (principalSource instanceof DataScopeAwarePrincipal dataScopeAwarePrincipal && dataScopeAwarePrincipal.getDataScope() != null) {
            attributes.put(SecurityAttributeKeys.DATA_SCOPE, dataScopeAwarePrincipal.getDataScope());
        }
        if (principalSource instanceof AdminAwarePrincipal adminAwarePrincipal && adminAwarePrincipal.getAdminFlag() != null) {
            attributes.put(SecurityAttributeKeys.IS_ADMIN, adminAwarePrincipal.getAdminFlag());
        }
        if (principalSource instanceof AuditAwarePrincipal auditAwarePrincipal && auditAwarePrincipal.getAuditUserId() != null) {
            attributes.put(SecurityAttributeKeys.AUDIT_USER_ID, auditAwarePrincipal.getAuditUserId());
        }
        return attributes;
    }
}
