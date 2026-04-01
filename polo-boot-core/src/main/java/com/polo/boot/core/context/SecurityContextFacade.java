package com.polo.boot.core.context;

public interface SecurityContextFacade {
    Long getTenantId();

    Long getDeptId();

    String getDataScope();

    Long getAuditUserId();

    Boolean isAdmin();

    static SecurityContextFacade none() {
        return new SecurityContextFacade() {
            @Override
            public Long getTenantId() {
                return null;
            }

            @Override
            public Long getDeptId() {
                return null;
            }

            @Override
            public String getDataScope() {
                return null;
            }

            @Override
            public Long getAuditUserId() {
                return null;
            }

            @Override
            public Boolean isAdmin() {
                return null;
            }
        };
    }
}
