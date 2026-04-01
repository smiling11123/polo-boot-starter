package com.polo.demo.security;

import com.polo.boot.security.annotation.SecurityAttributeField;
import com.polo.boot.security.annotation.SecurityAttributeType;
import com.polo.boot.security.model.LoginUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DemoLoginPrincipal extends LoginUser {
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
}
