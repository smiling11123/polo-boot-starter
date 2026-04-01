package com.polo.boot.security.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class LoginUser implements UserPrincipal{
    private Long userId;
    private String username;
    private String role;
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
