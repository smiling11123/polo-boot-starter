package com.polo.boot.storage.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "polo.storage.oss")
public class OSSProperties {

    /**
     * 是否启用 OSS 文件存储实现。
     */
    private boolean ossEnabled = false;

    /**
     * OSS 服务地址。
     */
    private String endPoint;

    /**
     * OSS 默认存储桶名称。
     */
    private String bucket;

    /**
     * OSS Access Key。
     */
    private String accessKey;

    /**
     * OSS Secret Key。
     */
    private String secretKey;

    /**
     * OSS 文件对外访问地址前缀。
     */
    private String publicUrl;
}
