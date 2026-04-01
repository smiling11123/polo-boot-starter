package com.polo.boot.security.support;

import com.polo.boot.security.annotation.Logical;

import java.util.Set;

public class PermissionMatcher {

    public boolean matches(Set<String> ownedPermissions, String[] requiredPermissions, Logical logical) {
        if (requiredPermissions == null || requiredPermissions.length == 0) {
            return true;
        }
        if (ownedPermissions == null || ownedPermissions.isEmpty()) {
            return false;
        }

        if (logical == Logical.ALL) {
            for (String requiredPermission : requiredPermissions) {
                if (!hasPermission(ownedPermissions, requiredPermission)) {
                    return false;
                }
            }
            return true;
        }

        for (String requiredPermission : requiredPermissions) {
            if (hasPermission(ownedPermissions, requiredPermission)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPermission(Set<String> ownedPermissions, String requiredPermission) {
        if (requiredPermission == null || requiredPermission.isBlank()) {
            return true;
        }
        if (ownedPermissions == null || ownedPermissions.isEmpty()) {
            return false;
        }
        for (String ownedPermission : ownedPermissions) {
            if (matchesPattern(ownedPermission, requiredPermission)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesPattern(String ownedPermission, String requiredPermission) {
        if (ownedPermission == null || ownedPermission.isBlank()) {
            return false;
        }
        if (ownedPermission.equals(requiredPermission) || ownedPermission.equals("*:*:*") || ownedPermission.equals("*")) {
            return true;
        }

        String[] ownedParts = ownedPermission.split(":");
        String[] requiredParts = requiredPermission.split(":");
        if (ownedParts.length != requiredParts.length) {
            return false;
        }

        for (int i = 0; i < ownedParts.length; i++) {
            if (!"*".equals(ownedParts[i]) && !ownedParts[i].equals(requiredParts[i])) {
                return false;
            }
        }
        return true;
    }
}
