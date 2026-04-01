package com.polo.boot.security.provider;

import com.polo.boot.core.context.CurrentPrincipal;
import com.polo.boot.core.context.CurrentPrincipalProvider;
import com.polo.boot.security.context.UserContext;
import com.polo.boot.security.model.UserPrincipal;

public class SecurityCurrentPrincipalProvider implements CurrentPrincipalProvider {
    @Override
    public CurrentPrincipal getCurrentPrincipal() {
        UserPrincipal userPrincipal = UserContext.get();
        if (userPrincipal == null) {
            return null;
        }
        return new CurrentPrincipal(
                userPrincipal.getPrincipalId(),
                userPrincipal.getPrincipalName(),
                userPrincipal.getPrincipalRole(),
                userPrincipal.isAdmin()
        );
    }
}
