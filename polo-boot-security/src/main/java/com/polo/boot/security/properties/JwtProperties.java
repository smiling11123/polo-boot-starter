package com.polo.boot.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "polo.security.jwt")
public class JwtProperties {
    /**
     * 是否启用服务端会话管理。
     * false 表示纯 JWT 无状态模式，true 表示使用 Redis 维护登录会话。
     */
    private Boolean sessionEnabled = false;

    /**
     * JWT 签名密钥，至少需要 32 个字节。
     */
    private String secret;

    /**
     * Access Token 有效期，单位为分钟。
     */
    private Long accessTokenExpire;

    /**
     * Refresh Token 有效期，单位为分钟。
     * 仅在开启双 token 模式时生效。
     */
    private Long refreshTokenExpire;

    /**
     * 是否启用 Refresh Token 轮换。
     * 开启后每次刷新都会签发新的 Refresh Token。
     */
    private Boolean refreshRotation;

    /**
     * 单个账号允许同时在线的最大设备数。
     * 小于等于 0 时表示不限制。
     */
    private Integer maxDevices;

    /**
     * 是否允许同一账号并发登录多个设备。
     */
    private Boolean allowConcurrentLogin;

    /**
     * 是否启用双 token 模式。
     * 开启后登录会返回 accessToken 和 refreshToken。
     */
    private Boolean allowTokenPair;
}
