package com.polo.boot.security.service;

import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.security.context.SecurityPrincipalAttributesResolver;
import com.polo.boot.security.model.LoginUser;
import com.polo.boot.security.model.TokenPair;
import com.polo.boot.security.model.UserPrincipal;
import com.polo.boot.security.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Date;

public class JwtService {
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_ATTRIBUTES = "ctx";
    public static final String CLAIM_SESSION_ID = "sid";
    public static final String CLAIM_DEVICE_ID = "did";
    public static final String CLAIM_TOKEN_TYPE = "typ";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        validateProperties();
    }

    public String createAccessToken(UserPrincipal userPrincipal, String sessionId, String deviceId) {
        return createToken(userPrincipal, jwtProperties.getAccessTokenExpire(), TOKEN_TYPE_ACCESS, sessionId, deviceId);
    }

    public String createRefreshToken(UserPrincipal userPrincipal, String sessionId, String deviceId) {
        return createToken(userPrincipal, jwtProperties.getRefreshTokenExpire(), TOKEN_TYPE_REFRESH, sessionId, deviceId);
    }

    private String createToken(UserPrincipal userPrincipal, Long expirationMinutes, String tokenType, String sessionId, String deviceId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMinutes * 60 * 1000);
        Map<String, Object> attributes = shouldWriteAttributesToToken()
                ? SecurityPrincipalAttributesResolver.resolve(userPrincipal)
                : Map.of();
        return Jwts.builder()
                .subject(String.valueOf(userPrincipal.getPrincipalId()))
                .claim(CLAIM_USERNAME, userPrincipal.getPrincipalName())
                .claim(CLAIM_ROLE, userPrincipal.getPrincipalRole())
                .claim(CLAIM_ATTRIBUTES, attributes == null || attributes.isEmpty() ? null : attributes)
                .claim(CLAIM_SESSION_ID, jwtProperties.getSessionEnabled() ? sessionId : null)
                .claim(CLAIM_DEVICE_ID, jwtProperties.getSessionEnabled() ? deviceId : null)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    public TokenPair createTokenPair(UserPrincipal userPrincipal, String sessionId, String deviceId) {
        String accessToken = createAccessToken(userPrincipal, sessionId, deviceId);
        String refreshToken = null;
        Long refreshExpire = null;
        if (Boolean.TRUE.equals(jwtProperties.getAllowTokenPair())) {
            refreshToken = createRefreshToken(userPrincipal, sessionId, deviceId);
            refreshExpire = jwtProperties.getRefreshTokenExpire();
        }
        return new TokenPair(
                accessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpire(),
                refreshExpire,
                "Bearer"
        );
    }

    public Claims parseAccessClaims(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, TOKEN_TYPE_ACCESS);
        return claims;
    }

    public Claims parseRefreshClaims(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, TOKEN_TYPE_REFRESH);
        return claims;
    }

    public UserPrincipal parse(String token) {
        return toUserPrincipal(parseAccessClaims(token));
    }

    public UserPrincipal toUserPrincipal(Claims claims) {
        if (!StringUtils.hasText(claims.getSubject())) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(Long.valueOf(claims.getSubject()));
        loginUser.setUsername((String) claims.get(CLAIM_USERNAME));
        loginUser.setRole((String) claims.get(CLAIM_ROLE));
        loginUser.setAttributes(extractAttributes(claims.get(CLAIM_ATTRIBUTES)));
        return loginUser;
    }


    private Claims parseClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!StringUtils.hasText(claims.getSubject())) {
                throw new BizException(ErrorCode.TOKEN_INVALID);
            }
            return claims;
        } catch (ExpiredJwtException e) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        } catch (BizException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
    }

    private void validateTokenType(Claims claims, String expectedType) {
        String actualType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!expectedType.equals(actualType)) {
            String msg = TOKEN_TYPE_REFRESH.equals(expectedType) ? "该令牌不是有效的刷新令牌" : "该令牌不是有效的访问令牌";
            throw new BizException(ErrorCode.TOKEN_INVALID.getCode(), msg);
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, Object> extractAttributes(Object rawAttributes) {
        if (!(rawAttributes instanceof Map<?, ?> rawMap) || rawMap.isEmpty()) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> attributes = new LinkedHashMap<>();
        rawMap.forEach((key, value) -> {
            if (key != null && value != null) {
                attributes.put(String.valueOf(key), value);
            }
        });
        return attributes;
    }

    private boolean shouldWriteAttributesToToken() {
        return !Boolean.TRUE.equals(jwtProperties.getSessionEnabled());
    }

    private void validateProperties() {
        if (!StringUtils.hasText(jwtProperties.getSecret())
                || jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("polo.security.jwt.secret 至少需要 32 个字节");
        }
        if (Boolean.TRUE.equals(jwtProperties.getAllowTokenPair())) {
            // Remove the strict session-enabled requirement
            // if (!Boolean.TRUE.equals(jwtProperties.getSessionEnabled())) { ... }
        }
        if (jwtProperties.getAccessTokenExpire() == null || jwtProperties.getAccessTokenExpire() <= 0) {
            throw new IllegalArgumentException("polo.security.jwt.access-token-expire 必须大于 0");
        }
        if (Boolean.TRUE.equals(jwtProperties.getAllowTokenPair())
                && (jwtProperties.getRefreshTokenExpire() == null || jwtProperties.getRefreshTokenExpire() <= 0)) {
            throw new IllegalArgumentException("polo.security.jwt.refresh-token-expire 必须大于 0");
        }
    }
}
