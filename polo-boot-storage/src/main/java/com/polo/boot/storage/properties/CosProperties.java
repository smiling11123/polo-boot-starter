package com.polo.boot.storage.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "polo.storage.cos")
public class CosProperties {
    /**
     * 是否启用腾讯云 COS 文件存储实现。
     */
    private boolean cosEnabled = false;

    /**
     * COS 所属地域，例如 ap-shanghai。
     */
    private String region;

    /**
     * COS 存储桶名称。
     */
    private String bucket;

    /**
     * 腾讯云 SecretId。
     */
    private String secretId;

    /**
     * 腾讯云 SecretKey。
     */
    private String secretKey;

    /**
     * COS 文件对外访问地址前缀。
     */
    private String publicUrl;
}
