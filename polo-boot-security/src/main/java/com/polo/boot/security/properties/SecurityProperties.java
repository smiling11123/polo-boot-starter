package com.polo.boot.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "polo.security")
public class SecurityProperties {
    /**
     * 是否启用安全模块自动装配。
     */
    private boolean enabled = true;

    /**
     * 是否启用认证主链路，包括 JWT、拦截器和会话管理。
     */
    private boolean authEnabled = true;

    /**
     * 是否启用 @CurrentUser 和 @CurrentUserAttribute 参数解析器。
     */
    private boolean currentUserResolverEnabled = true;

    /**
     * 是否启用防重复提交切面。
     */
    private boolean duplicateSubmitEnabled = true;

    /**
     * 是否启用基于 Redis 的接口限流能力。
     */
    private boolean rateLimitEnabled = true;

    /**
     * 是否启用二维码登录能力。
     */
    private boolean qrCodeEnabled = true;
}
