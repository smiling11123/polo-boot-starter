package com.polo.boot.core.context;

public record CurrentPrincipal(Long principalId,
                               String principalName,
                               String principalRole,
                               boolean admin) {
}
