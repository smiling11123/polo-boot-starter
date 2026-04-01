package com.polo.boot.security.model;

import java.util.Map;

public interface SecurityAttributeProvider {
    Map<String, Object> provideSecurityAttributes();
}
