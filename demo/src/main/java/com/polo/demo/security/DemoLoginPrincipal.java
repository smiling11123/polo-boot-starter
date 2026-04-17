package com.polo.demo.security;

import com.polo.boot.security.annotation.SecurityAttributesField;
import com.polo.boot.security.annotation.SecurityAttributeField;
import com.polo.boot.security.annotation.SecurityAttributeType;
import com.polo.boot.security.annotation.SecurityPrincipalField;
import com.polo.boot.security.annotation.SecurityPrincipalType;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class DemoLoginPrincipal {
    @SecurityPrincipalField(type = SecurityPrincipalType.PRINCIPAL_ID)

    private Long userId;

    @SecurityPrincipalField(type = SecurityPrincipalType.PRINCIPAL_NAME)
    private String username;

    private String password;

    @SecurityPrincipalField(type = SecurityPrincipalType.PRINCIPAL_ROLE)
    private String role;

    @SecurityAttributeField(type = SecurityAttributeType.TENANT_ID)
    private Long tenantId;

    @SecurityAttributeField(type = SecurityAttributeType.DEPT_ID)
    private Long deptId;

    @SecurityAttributeField(type = SecurityAttributeType.DATA_SCOPE)
    private String dataScope;

    @SecurityAttributeField(type = SecurityAttributeType.AUDIT_USER_ID)
    private Long auditUserId;

    @SecurityAttributeField(type = SecurityAttributeType.IS_ADMIN)
    private Boolean adminFlag;

    @SecurityAttributeField(type = SecurityAttributeType.PERMISSIONS)
    private List<String> permissions;

    @SecurityAttributesField
    private Map<String, Object> attributes = new LinkedHashMap<>();
}
