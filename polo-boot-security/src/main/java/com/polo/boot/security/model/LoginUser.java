package com.polo.boot.security.model;

import com.polo.boot.security.annotation.SecurityAttributesField;
import com.polo.boot.security.annotation.SecurityPrincipalField;
import com.polo.boot.security.annotation.SecurityPrincipalType;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class LoginUser implements UserPrincipal{
    @SecurityPrincipalField(type = SecurityPrincipalType.PRINCIPAL_ID)
    private Long userId;

    @SecurityPrincipalField(type = SecurityPrincipalType.PRINCIPAL_NAME)
    private String username;

    @SecurityPrincipalField(type = SecurityPrincipalType.PRINCIPAL_ROLE)
    private String role;

    @SecurityAttributesField
    private Map<String, Object> attributes = new LinkedHashMap<>();

    @Override
    public String getPrincipalName() {
        return username;
    }

    @Override
    public Long getPrincipalId() {
        return userId;
    }

    @Override
    public String getPrincipalRole() {
        return role;
    }

    public void putAttribute(String key, Object value) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }
        attributes.put(key, value);
    }
}
