package com.polo.boot.security.provider;

import com.polo.boot.security.context.UserContext;
import com.polo.boot.security.model.UserPrincipal;
import com.polo.boot.web.spi.OperatorContext;
import com.polo.boot.web.spi.OperatorContextProvider;

public class SecurityOperatorContextProvider implements OperatorContextProvider {
    @Override
    public OperatorContext getCurrentOperator() {
        UserPrincipal userPrincipal = UserContext.get();
        if (userPrincipal == null) {
            return OperatorContext.anonymous();
        }
        return new OperatorContext(userPrincipal.getPrincipalId(), userPrincipal.getPrincipalName());
    }
}
