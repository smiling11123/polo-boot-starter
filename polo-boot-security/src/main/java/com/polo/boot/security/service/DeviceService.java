package com.polo.boot.security.service;

import com.polo.boot.core.util.IpUtil;
import com.polo.boot.security.model.ClientDevice;
import com.polo.boot.security.model.DeviceInfo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class DeviceService {

    public DeviceInfo resolve(HttpServletRequest request) {
        return resolve(request, null);
    }

    public DeviceInfo resolve(ClientDevice clientDevice) {
        return resolve(null, clientDevice);
    }

    public DeviceInfo resolve(HttpServletRequest request, ClientDevice clientDevice) {
        DeviceInfo deviceInfo = new DeviceInfo();
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        String ip = request != null ? IpUtil.getClientIp(request) : null;

        String deviceType = normalizeDeviceType(firstNonBlank(
                clientDevice != null ? clientDevice.getDeviceType() : null,
                request != null ? request.getHeader("X-Platform") : null,
                request != null ? request.getHeader("Sec-CH-UA-Platform") : null,
                inferDeviceType(userAgent)
        ));

        String deviceId = firstNonBlank(
                clientDevice != null ? clientDevice.getDeviceId() : null,
                request != null ? request.getHeader("X-Device-Id") : null,
                request != null ? getCookieValue(request, "DEVICE_ID") : null,
                buildFallbackDeviceId(deviceType, userAgent, ip)
        );

        String deviceName = firstNonBlank(
                clientDevice != null ? clientDevice.getDeviceName() : null,
                request != null ? request.getHeader("X-Device-Name") : null,
                buildDefaultDeviceName(deviceType, userAgent)
        );

        deviceInfo.setDeviceId(deviceId);
        deviceInfo.setDeviceType(deviceType);
        deviceInfo.setDeviceName(deviceName);
        deviceInfo.setUserAgent(userAgent);
        deviceInfo.setIp(ip);
        return deviceInfo;
    }

    private String buildFallbackDeviceId(String deviceType, String userAgent, String ip) {
        if (!StringUtils.hasText(userAgent) && !StringUtils.hasText(ip)) {
            return UUID.randomUUID().toString();
        }
        String raw = String.format("%s|%s|%s",
                StringUtils.hasText(deviceType) ? deviceType : "unknown",
                StringUtils.hasText(userAgent) ? userAgent : "unknown",
                StringUtils.hasText(ip) ? ip : "unknown");
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String buildDefaultDeviceName(String deviceType, String userAgent) {
        if (StringUtils.hasText(deviceType)) {
            return deviceType + " client";
        }
        if (StringUtils.hasText(userAgent)) {
            return userAgent.length() > 60 ? userAgent.substring(0, 60) : userAgent;
        }
        return "unknown client";
    }

    private String inferDeviceType(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "unknown";
        }
        String normalized = userAgent.toLowerCase();
        if (normalized.contains("iphone") || normalized.contains("ios")) {
            return "ios";
        }
        if (normalized.contains("android")) {
            return "android";
        }
        if (normalized.contains("windows") || normalized.contains("macintosh") || normalized.contains("linux")) {
            return "web";
        }
        return "unknown";
    }

    private String normalizeDeviceType(String deviceType) {
        if (!StringUtils.hasText(deviceType)) {
            return "unknown";
        }
        return deviceType.replace("\"", "").trim().toLowerCase();
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
