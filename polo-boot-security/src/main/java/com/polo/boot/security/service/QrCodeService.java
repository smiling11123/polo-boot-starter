package com.polo.boot.security.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.security.context.SecurityPrincipalSupport;
import com.polo.boot.security.model.ClientDevice;
import com.polo.boot.security.model.LoginUser;
import com.polo.boot.security.model.QrCodeStatus;
import com.polo.boot.security.model.TokenPair;
import com.polo.boot.security.model.UserPrincipal;
import com.polo.boot.security.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class QrCodeService {
    private final StringRedisTemplate redisTemplate;
    // 使用 ObjectProvider 因为当 session-enabled=false 时，TokenService 将无法被注入
    private final ObjectProvider<TokenService> tokenServiceProvider;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;

    private static final String QR_PREFIX = "login:qrcode:";

    public String generateQrCode() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForHash().put(QR_PREFIX + uuid, "status", QrCodeStatus.WAITING_SCAN.name());
        redisTemplate.expire(QR_PREFIX + uuid, Duration.ofMinutes(3));
        return uuid;
    }

    public Map<String, Object> checkStatus(String uuid) {
        String status = (String) redisTemplate.opsForHash().get(QR_PREFIX + uuid, "status");
        if (status == null) {
            return Map.of("status", QrCodeStatus.EXPIRED.name());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", status);

        if (QrCodeStatus.CONFIRMED.name().equals(status)) {
            String tokenPairJson = (String) redisTemplate.opsForHash().get(QR_PREFIX + uuid, "tokenPair");
            if (tokenPairJson != null) {
                try {
                    // 返还完整的双 Token
                    result.put("tokenPair", objectMapper.readValue(tokenPairJson, TokenPair.class));
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse token pair json", e);
                }
            } else {
                // 兼容性
                result.put("token", redisTemplate.opsForHash().get(QR_PREFIX + uuid, "token"));
            }
            redisTemplate.delete(QR_PREFIX + uuid);
        } else if (QrCodeStatus.SCANNED.name().equals(status)) {
            Object avatar = redisTemplate.opsForHash().get(QR_PREFIX + uuid, "avatar");
            Object nickname = redisTemplate.opsForHash().get(QR_PREFIX + uuid, "nickname");
            if (avatar != null) result.put("avatar", avatar);
            if (nickname != null) result.put("nickname", nickname);
        }
        return result;
    }

    public void scan(String uuid, LoginUser currentUser) {
        scan(uuid, (Object) currentUser);
    }

    public void scan(String uuid, Object currentUser) {
        UserPrincipal userPrincipal = SecurityPrincipalSupport.adapt(currentUser);
        String status = (String) redisTemplate.opsForHash().get(QR_PREFIX + uuid, "status");
        if (!QrCodeStatus.WAITING_SCAN.name().equals(status)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "二维码已失效或已被扫描");
        }
        redisTemplate.opsForHash().put(QR_PREFIX + uuid, "status", QrCodeStatus.SCANNED.name());
        redisTemplate.opsForHash().put(QR_PREFIX + uuid, "userId", userPrincipal.getPrincipalId().toString());
        redisTemplate.opsForHash().put(QR_PREFIX + uuid, "nickname", userPrincipal.getPrincipalName());

        Object avatar = userPrincipal.getAttributes().get("avatar");
        if (avatar != null) {
            redisTemplate.opsForHash().put(QR_PREFIX + uuid, "avatar", avatar.toString());
        }
    }

    public TokenPair confirm(String uuid, LoginUser currentUser) {
        return confirm(uuid, (Object) currentUser);
    }

    public TokenPair confirm(String uuid, Object currentUser) {
        UserPrincipal userPrincipal = SecurityPrincipalSupport.adapt(currentUser);
        String userIdStr = (String) redisTemplate.opsForHash().get(QR_PREFIX + uuid, "userId");
        if (userIdStr == null || !userIdStr.equals(userPrincipal.getPrincipalId().toString())) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "二维码状态异动或您不是扫码本人");
        }

        TokenService tokenService = tokenServiceProvider.getIfAvailable();
        TokenPair tokenPair;
        
        if (tokenService != null) {
            // Session、多设备、双Token 都已在其内部处理
            ClientDevice webDevice = new ClientDevice();
            webDevice.setDeviceId("WEB-" + UUID.randomUUID().toString().substring(0, 8));
            webDevice.setDeviceType("PC");
            webDevice.setDeviceName("Web QrCode Login");

            tokenPair = tokenService.login(userPrincipal, webDevice);
        } else {
            String deviceId = "WEB-" + UUID.randomUUID().toString().substring(0, 8);
            tokenPair = jwtService.createTokenPair(userPrincipal, null, deviceId);
        }
        
        redisTemplate.opsForHash().put(QR_PREFIX + uuid, "status", QrCodeStatus.CONFIRMED.name());
        
        try {
            // 保证 PC 端拿到全量 TokenPair，特别是刷新令牌 
            redisTemplate.opsForHash().put(QR_PREFIX + uuid, "tokenPair", objectMapper.writeValueAsString(tokenPair));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize token pair", e);
            redisTemplate.opsForHash().put(QR_PREFIX + uuid, "token", tokenPair.getAccessToken());
        }

        return tokenPair;
    }
}
