package com.polo.boot.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.security.context.SecurityPrincipalAttributesResolver;
import com.polo.boot.security.model.ClientDevice;
import com.polo.boot.security.model.DeviceInfo;
import com.polo.boot.security.model.LoginSession;
import com.polo.boot.security.model.LoginUser;
import com.polo.boot.security.model.TokenPair;
import com.polo.boot.security.model.UserPrincipal;
import com.polo.boot.security.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TokenService {
    private static final String SESSION_KEY_PATTERN = "polo:security:session:%d:%s";
    private static final String USER_SESSIONS_KEY_PATTERN = "polo:security:user-sessions:%d";
    private static final long LAST_ACTIVE_FLUSH_INTERVAL_MS = 60_000L;
    private final JwtService jwtService;
    private final DeviceService deviceService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final JwtProperties jwtProperties;

    public TokenService(JwtService jwtService,
                        DeviceService deviceService,
                        StringRedisTemplate stringRedisTemplate,
                        ObjectMapper objectMapper,
                        JwtProperties jwtProperties) {
        this.jwtService = jwtService;
        this.deviceService = deviceService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.jwtProperties = jwtProperties;
    }

    public TokenPair login(UserPrincipal userPrincipal, HttpServletRequest request) {
        return login(userPrincipal, request, null);
    }

    public TokenPair login(UserPrincipal userPrincipal, ClientDevice clientDevice) {
        return login(userPrincipal, null, clientDevice);
    }

    public TokenPair login(UserPrincipal userPrincipal, HttpServletRequest request, ClientDevice clientDevice) {
        Long userId = userPrincipal.getPrincipalId();


        if(sessionEnable()){
            DeviceInfo currentDevice = deviceService.resolve(request, clientDevice);
            cleanupStaleSessions(userId);

            if (!allowConcurrentLogin()) {
                logoutAll(userId, null);
            } else {
                revokeSameDeviceSessions(userId, currentDevice.getDeviceId());
                evictOverflowSessions(userId);
            }

            String sessionId = UUID.randomUUID().toString();
            TokenPair tokenPair = issueTokenPair(userPrincipal, sessionId, currentDevice.getDeviceId());
            long now = System.currentTimeMillis();
            LoginSession session = new LoginSession(
                    sessionId,
                    userId,
                    userPrincipal.getPrincipalName(),
                    userPrincipal.getPrincipalRole(),
                    currentDevice.getDeviceId(),
                    currentDevice.getDeviceType(),
                    currentDevice.getDeviceName(),
                    currentDevice.getUserAgent(),
                    currentDevice.getIp(),
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    tokenPair.getRefreshToken() != null ? fingerprint(tokenPair.getRefreshToken()) : null,
                    tokenPair.getRefreshToken() != null ? now + jwtProperties.getRefreshTokenExpire() * 60 * 1000 : null,
                    fingerprint(tokenPair.getAccessToken()),
                    now + jwtProperties.getAccessTokenExpire() * 60 * 1000,
                    extractAttributes(userPrincipal)
            );
            saveSession(session);
            return tokenPair;
        }
        return issueTokenPair(userPrincipal, null, null);
    }

    public TokenPair refresh(String refreshToken) {

        Claims claims = allowTokenPair() ? jwtService.parseRefreshClaims(refreshToken) : jwtService.parseAccessClaims(refreshToken);
        Long userId = Long.valueOf(claims.getSubject());
        String sessionId = claims.get(JwtService.CLAIM_SESSION_ID, String.class);
        LoginSession session = getSession(userId, sessionId);
        if (sessionEnable() && session == null) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        }
        if (sessionEnable() && !fingerprint(refreshToken).equals(allowTokenPair() ? session.getRefreshTokenHash() : session.getAccessTokenHash())) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }

        long now = System.currentTimeMillis();
        if (sessionEnable() && session.getRefreshExpiresAt() != null && session.getRefreshExpiresAt() <= now) {
            deleteSession(userId, sessionId);
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        }
        LoginUser loginUser;
        if (session != null) {
            loginUser = toLoginUser(session);
        } else {
            // 无状态模式：从 Claims 还原用户信息
            UserPrincipal principal = jwtService.toUserPrincipal(claims);
            loginUser = new LoginUser();
            loginUser.setUserId(principal.getPrincipalId());
            loginUser.setUsername(principal.getPrincipalName());
            loginUser.setRole(principal.getPrincipalRole());
        }

        String sessionIdForTokens = session != null ? session.getSessionId() : null;
        String deviceIdForTokens = session != null ? session.getDeviceId() : null;

        if (!allowTokenPair()) {
            return jwtService.createTokenPair(loginUser, sessionIdForTokens, deviceIdForTokens);
        }

        String accessToken = jwtService.createAccessToken(loginUser, sessionIdForTokens, deviceIdForTokens);
        String responseRefreshToken = refreshToken;

        if (refreshRotation()) {
            responseRefreshToken = jwtService.createRefreshToken(loginUser, sessionIdForTokens, deviceIdForTokens);
            if (session != null) {
                session.setRefreshTokenHash(fingerprint(responseRefreshToken));
                session.setRefreshExpiresAt(now + jwtProperties.getRefreshTokenExpire() * 60 * 1000);
            }
        }

        if (session != null) {
            session.setLastActiveTime(LocalDateTime.now());
            saveSession(session);
        }

        return new TokenPair(
                accessToken,
                responseRefreshToken,
                jwtProperties.getAccessTokenExpire(),
                jwtProperties.getRefreshTokenExpire(),
                "Bearer"
        );
    }

    public UserPrincipal authenticate(String accessToken) {
        Claims claims = jwtService.parseAccessClaims(accessToken);
        Long userId = Long.valueOf(claims.getSubject());
        String sessionId = claims.get(JwtService.CLAIM_SESSION_ID, String.class);
        String deviceId = claims.get(JwtService.CLAIM_DEVICE_ID, String.class);

        LoginSession session = getSession(userId, sessionId);
        if (sessionEnable() && session == null) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        }
        if (session != null) {
            if (session.getDeviceId() != null && deviceId != null && !session.getDeviceId().equals(deviceId)) {
                throw new BizException(ErrorCode.TOKEN_INVALID);
            }

            long now = System.currentTimeMillis();
            LocalDateTime lastActive = session.getLastActiveTime();
            if (lastActive == null || ChronoUnit.MILLIS.between(lastActive, LocalDateTime.now()) >= LAST_ACTIVE_FLUSH_INTERVAL_MS) {
                session.setLastActiveTime(LocalDateTime.now());
                saveSession(session);
            }
            return toLoginUser(session);
        }

        // 无状态模式（静默降级或显式关闭会话）：直接从 Claims 还原用户 Principal
        return jwtService.toUserPrincipal(claims);
    }

    public List<DeviceInfo> listDevices(Long userId, String currentSessionId) {
        if(!jwtProperties.getSessionEnabled()) throw new BizException(ErrorCode.SYSTEM_ERROR.getCode(), "未开启多设备会话管理");
        List<LoginSession> sessions = getActiveSessions(userId);
        return sessions.stream()
                .sorted(Comparator.comparing(LoginSession::getLastActiveTime, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .map(session -> new DeviceInfo(
                        session.getSessionId(),
                        session.getDeviceId(),
                        session.getDeviceType(),
                        session.getDeviceName(),
                        session.getUserAgent(),
                        session.getIp(),
                        session.getLoginTime(),
                        session.getLastActiveTime(),
                        session.getSessionId().equals(currentSessionId)
                ))
                .toList();
    }

    public void logout(String accessToken) {
        if(!jwtProperties.getSessionEnabled()) throw new BizException(ErrorCode.SYSTEM_ERROR.getCode(), "未开启多设备会话管理");
        Claims claims = jwtService.parseAccessClaims(accessToken);
        Long userId = Long.valueOf(claims.getSubject());
        String sessionId = claims.get(JwtService.CLAIM_SESSION_ID, String.class);
        logoutDevice(userId, sessionId);
    }

    public void logoutDevice(Long userId, String sessionId) {
        if(!jwtProperties.getSessionEnabled()) throw new BizException(ErrorCode.SYSTEM_ERROR.getCode(), "未开启多设备会话管理");
        deleteSession(userId, sessionId);
    }

    public void logoutAll(Long userId, String keepSessionId) {
        if(!jwtProperties.getSessionEnabled()) throw new BizException(ErrorCode.SYSTEM_ERROR.getCode(), "未开启多设备会话管理");
        for (LoginSession session : getActiveSessions(userId)) {
            if (keepSessionId != null && keepSessionId.equals(session.getSessionId())) {
                continue;
            }
            deleteSession(userId, session.getSessionId());
        }
    }

    public String resolveCurrentSessionId(String accessToken) {
        if(!jwtProperties.getSessionEnabled()) throw new BizException(ErrorCode.SYSTEM_ERROR.getCode(), "未开启多设备会话管理");
        return jwtService.parseAccessClaims(accessToken).get(JwtService.CLAIM_SESSION_ID, String.class);
    }

    private TokenPair issueTokenPair(UserPrincipal userPrincipal, String sessionId, String deviceId) {
        String accessToken = jwtService.createAccessToken(userPrincipal, sessionId, deviceId);
        if (!allowTokenPair()) {
            return new TokenPair(accessToken, null, jwtProperties.getAccessTokenExpire(), null, "Bearer");
        }

        String refreshToken = jwtService.createRefreshToken(userPrincipal, sessionId, deviceId);
        return new TokenPair(
                accessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpire(),
                jwtProperties.getRefreshTokenExpire(),
                "Bearer"
        );
    }

    private void saveSession(LoginSession session) {
        String key = buildSessionKey(session.getUserId(), session.getSessionId());
        long ttlMinutes = resolveSessionTtlMinutes(session);
        if (ttlMinutes <= 0) {
            deleteSession(session.getUserId(), session.getSessionId());
            return;
        }
        String userSessionsKey = buildUserSessionsKey(session.getUserId());
        stringRedisTemplate.opsForValue().set(key, serialize(session), ttlMinutes, TimeUnit.MINUTES);
        stringRedisTemplate.opsForSet().add(userSessionsKey, session.getSessionId());
        stringRedisTemplate.expire(userSessionsKey, ttlMinutes, TimeUnit.MINUTES);
    }

    private LoginSession getSession(Long userId, String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
        String key = buildSessionKey(userId, sessionId);
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            stringRedisTemplate.opsForSet().remove(buildUserSessionsKey(userId), sessionId);
            return null;
        }
        return deserialize(json);
    }

    private List<LoginSession> getActiveSessions(Long userId) {
        Set<String> sessionIds = stringRedisTemplate.opsForSet().members(buildUserSessionsKey(userId));
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }

        List<LoginSession> sessions = new ArrayList<>();
        for (String sessionId : sessionIds) {
            LoginSession session = getSession(userId, sessionId);
            if (session != null) {
                sessions.add(session);
            }
        }
        return sessions;
    }

    private void cleanupStaleSessions(Long userId) {
        getActiveSessions(userId);
    }

    private void revokeSameDeviceSessions(Long userId, String deviceId) {
        if (deviceId == null) {
            return;
        }
        for (LoginSession session : getActiveSessions(userId)) {
            if (deviceId.equals(session.getDeviceId())) {
                deleteSession(userId, session.getSessionId());
            }
        }
    }

    private void evictOverflowSessions(Long userId) {
        Integer maxDevices = jwtProperties.getMaxDevices();
        if (maxDevices == null || maxDevices <= 0) {
            return;
        }

        List<LoginSession> sessions = getActiveSessions(userId).stream()
                .sorted(Comparator.comparing(LoginSession::getLoginTime))
                .toList();
        int maxExistingSessions = Math.max(maxDevices - 1, 0);
        for (int i = 0; i < sessions.size() - maxExistingSessions; i++) {
            deleteSession(userId, sessions.get(i).getSessionId());
        }
    }

    private void deleteSession(Long userId, String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        String userSessionsKey = buildUserSessionsKey(userId);
        stringRedisTemplate.delete(buildSessionKey(userId, sessionId));
        stringRedisTemplate.opsForSet().remove(userSessionsKey, sessionId);
        Long remaining = stringRedisTemplate.opsForSet().size(userSessionsKey);
        if (remaining != null && remaining <= 0) {
            stringRedisTemplate.delete(userSessionsKey);
        }
    }

    private String buildSessionKey(Long userId, String sessionId) {
        return SESSION_KEY_PATTERN.formatted(userId, sessionId);
    }

    private String buildUserSessionsKey(Long userId) {
        return USER_SESSIONS_KEY_PATTERN.formatted(userId);
    }

    private String serialize(LoginSession session) {
        try {
            return objectMapper.writeValueAsString(session);
        } catch (Exception e) {
            throw new IllegalStateException("会话序列化失败", e);
        }
    }

    private LoginSession deserialize(String json) {
        try {
            return objectMapper.readValue(json, LoginSession.class);
        } catch (Exception e) {
            throw new IllegalStateException("会话反序列化失败", e);
        }
    }

    private LoginUser toLoginUser(LoginSession session) {
        if (session == null) {
            return null;
        }
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(session.getUserId());
        loginUser.setUsername(session.getUsername());
        loginUser.setRole(session.getRole());
        loginUser.setAttributes(session.getAttributes() != null ? new LinkedHashMap<>(session.getAttributes()) : new LinkedHashMap<>());
        return loginUser;
    }

    private String fingerprint(String token) {
        return DigestUtils.md5DigestAsHex(token.getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, Object> extractAttributes(UserPrincipal userPrincipal) {
        Map<String, Object> attributes = SecurityPrincipalAttributesResolver.resolve(userPrincipal);
        return attributes == null || attributes.isEmpty() ? null : new LinkedHashMap<>(attributes);
    }

    private long resolveSessionTtlMinutes(LoginSession session) {
        if (allowTokenPair() && session.getRefreshExpiresAt() != null) {
            long remainingMillis = session.getRefreshExpiresAt() - System.currentTimeMillis();
            if (remainingMillis <= 0) {
                return 0;
            }
            return Math.max(1, TimeUnit.MILLISECONDS.toMinutes(remainingMillis) + (remainingMillis % 60_000 == 0 ? 0 : 1));
        }
        return jwtProperties.getAccessTokenExpire();
    }

    private boolean allowTokenPair() {
        return Boolean.TRUE.equals(jwtProperties.getAllowTokenPair());
    }

    private boolean refreshRotation() {
        return Boolean.TRUE.equals(jwtProperties.getRefreshRotation());
    }

    private boolean allowConcurrentLogin() {
        return Boolean.TRUE.equals(jwtProperties.getAllowConcurrentLogin());
    }

    private  boolean sessionEnable() {
        return Boolean.TRUE.equals(jwtProperties.getSessionEnabled());
    }
}
