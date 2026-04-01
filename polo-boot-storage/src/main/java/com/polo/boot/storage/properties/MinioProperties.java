package com.polo.boot.storage.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "polo.storage.minio")
public class MinioProperties {
    /**
     * 是否启用 MinIO 默认实现。
     */
    private boolean minioEnabled = true;

    /**
     * MinIO 服务地址，例如 http://127.0.0.1:9000。
     */
    private String endpoint;

    /**
     * MinIO Access Key。
     */
    private String accessKey;

    /**
     * MinIO Secret Key。
     */
    private String secretKey;

    /**
     * 默认文件桶名称。
     */
    private String bucket;

    /**
     * 对外访问文件时使用的公网地址。
     * 未配置时通常回退为 endpoint。
     */
    private String publicUrl;
}
