package com.polo.boot.security.provider;

import com.polo.boot.security.model.PermissionProfile;

public interface AuthorizationProvider {
    PermissionProfile loadByUserId(Long userId);
}
