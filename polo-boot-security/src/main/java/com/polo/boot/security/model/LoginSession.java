package com.polo.boot.security.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.polo.boot.security.support.TimestampLocalDateTimeDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginSession {
    private String sessionId;
    private Long userId;
    private String username;
    private String role;
    private String deviceId;
    private String deviceType;
    private String deviceName;
    private String userAgent;
    private String ip;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = TimestampLocalDateTimeDeserializer.class)
    private LocalDateTime loginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = TimestampLocalDateTimeDeserializer.class)
    private LocalDateTime lastActiveTime;
    private String refreshTokenHash;
    private Long refreshExpiresAt;
    private String accessTokenHash;
    private Long accessExpiresAt;
    private Map<String, Object> attributes;
}
