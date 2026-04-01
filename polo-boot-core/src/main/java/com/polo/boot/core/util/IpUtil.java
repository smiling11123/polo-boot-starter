package com.polo.boot.core.util;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public class IpUtil {

    /**
     * 获取客户端真实IP地址
     * 支持多级代理：Nginx、CDN、WAF等
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = null;

        // 按优先级依次检查各种代理头
        String[] headers = {
                "X-Forwarded-For",      // 标准代理头
                "X-Real-IP",            // Nginx常用
                "Proxy-Client-IP",      // Apache
                "WL-Proxy-Client-IP",   // WebLogic
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"           // 最后兜底
        };

        for (String header : headers) {
            ip = request.getHeader(header);
            if (isValidIp(ip)) {
                // X-Forwarded-For可能包含多个IP，取第一个（最原始客户端）
                if ("X-Forwarded-For".equalsIgnoreCase(header) && ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                break;
            }
        }

        // 如果没有代理头，使用直接连接的IP
        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理IPv6本地地址
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }

    /**
     * 验证IP是否有效
     */
    private static boolean isValidIp(String ip) {
        return StringUtils.hasText(ip)
                && !"unknown".equalsIgnoreCase(ip)
                && !"0:0:0:0:0:0:0:1".equals(ip)
                && ip.length() <= 45; // IPv6最长45字符
    }

    /**
     * 获取IP的整数表示（用于布隆过滤器等）
     */
    public static long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return 0;

        return (Long.parseLong(parts[0]) << 24)
                | (Long.parseLong(parts[1]) << 16)
                | (Long.parseLong(parts[2]) << 8)
                | Long.parseLong(parts[3]);
    }
}
