package com.polo.boot.security.model;

import java.util.Collections;
import java.util.Set;

public record PermissionProfile(
        Long userId,
        String username,
        Set<String> roles,
        Set<String> permissions
) {
    public PermissionProfile {
        roles = roles == null ? Collections.emptySet() : Set.copyOf(roles);
        permissions = permissions == null ? Collections.emptySet() : Set.copyOf(permissions);
    }
}
