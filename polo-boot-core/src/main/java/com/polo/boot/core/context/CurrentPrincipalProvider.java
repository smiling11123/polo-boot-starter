package com.polo.boot.core.context;

public interface CurrentPrincipalProvider {
    CurrentPrincipal getCurrentPrincipal();

    static CurrentPrincipalProvider none() {
        return () -> null;
    }
}
