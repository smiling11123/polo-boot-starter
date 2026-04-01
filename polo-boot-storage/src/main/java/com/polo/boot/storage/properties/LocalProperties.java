package com.polo.boot.storage.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "polo.storage.local")
public class LocalProperties {

    /**
     * 是否启用本地文件存储实现。
     */
    private boolean localEnabled = false;

    /**
     * 本地文件根目录。
     */
    private String localPath;

    /**
     * 本地签名 URL 使用的安全密钥。
     */
    private String securityKey;
}
