package com.polo.boot.security.interceptor;

import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import org.springframework.util.StringUtils;

public final class TokenResolver {
    private static final String BEARER_PREFIX = "Bearer ";

    private TokenResolver() {
    }

    public static String resolveBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            return null;
        }

        String token = authorizationHeader.trim();
        if (token.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            token = token.substring(BEARER_PREFIX.length()).trim();
        } else if (token.contains(" ")) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }

        if (!StringUtils.hasText(token)) {
            throw new BizException(ErrorCode.TOKEN_MISSING);
        }
        return token;
    }
}
