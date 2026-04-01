package com.polo.boot.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenPair {
    private String accessToken;      // 访问令牌
    private String refreshToken;     // 刷新令牌
    private Long accessTokenExpire;  // Access 过期时间（秒）
    private Long refreshTokenExpire; // Refresh 过期时间（秒）
    private String tokenType;        // Bearer


}
