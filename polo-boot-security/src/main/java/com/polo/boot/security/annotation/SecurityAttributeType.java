package com.polo.boot.security.annotation;

import com.polo.boot.security.context.SecurityAttributeKeys;

public enum SecurityAttributeType {
    TENANT_ID(SecurityAttributeKeys.TENANT_ID),
    DEPT_ID(SecurityAttributeKeys.DEPT_ID),
    DATA_SCOPE(SecurityAttributeKeys.DATA_SCOPE),
    IS_ADMIN(SecurityAttributeKeys.IS_ADMIN),
    AUDIT_USER_ID(SecurityAttributeKeys.AUDIT_USER_ID),
    PERMISSIONS(SecurityAttributeKeys.PERMISSIONS);

    private final String attributeKey;

    SecurityAttributeType(String attributeKey) {
        this.attributeKey = attributeKey;
    }

    public String getAttributeKey() {
        return attributeKey;
    }
}
