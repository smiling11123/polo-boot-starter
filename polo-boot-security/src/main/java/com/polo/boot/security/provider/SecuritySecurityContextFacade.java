package com.polo.boot.security.provider;

import com.polo.boot.core.context.SecurityContextFacade;
import com.polo.boot.security.context.SecurityAttributeKeys;
import com.polo.boot.security.context.UserContext;

public class SecuritySecurityContextFacade implements SecurityContextFacade {
    @Override
    public Long getTenantId() {
        return UserContext.getAttribute(SecurityAttributeKeys.TENANT_ID, Long.class);
    }

    @Override
    public Long getDeptId() {
        return UserContext.getAttribute(SecurityAttributeKeys.DEPT_ID, Long.class);
    }

    @Override
    public String getDataScope() {
        return UserContext.getAttribute(SecurityAttributeKeys.DATA_SCOPE, String.class);
    }

    @Override
    public Long getAuditUserId() {
        return UserContext.getAttribute(SecurityAttributeKeys.AUDIT_USER_ID, Long.class);
    }

    @Override
    public Boolean isAdmin() {
        return UserContext.getAttribute(SecurityAttributeKeys.IS_ADMIN, Boolean.class);
    }
}
