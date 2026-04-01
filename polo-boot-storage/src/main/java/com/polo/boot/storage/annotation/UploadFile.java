package com.polo.boot.storage.annotation;

import java.lang.annotation.*;

/**
 * 文件上传注解（标记在 Controller 方法参数上）
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UploadFile {

    /**
     * Multipart 表单字段名。
     * 为空时优先使用方法参数名，最后回退为 file。
     */
    String value() default "";

    /**
     * 是否必须上传文件。
     */
    boolean required() default true;

    /**
     * 文件类型白名单（MIME 类型，如 image/jpeg, application/pdf）
     */
    String[] allowedTypes() default {};

    /**
     * 文件扩展名白名单（如 jpg, png, pdf）
     */
    String[] allowedExtensions() default {};

    /**
     * 最大文件大小（字节，-1 表示不限制）
     */
    long maxSize() default -1;

    /**
     * 是否生成缩略图（仅图片）
     */
    Toggle generateThumbnail() default Toggle.DEFAULT;

    /**
     * 缩略图宽度
     */
    int thumbnailWidth() default -1;

    /**
     * 缩略图高度
     */
    int thumbnailHeight() default -1;

    /**
     * 存储策略
     */
    StorageType storage() default StorageType.DEFAULT;

    /**
     * 存储路径前缀
     */
    String pathPrefix() default "";

    /**
     * 是否生成签名 URL（私有访问）
     */
    Toggle privateAccess() default Toggle.DEFAULT;

    /**
     * 签名 URL 过期时间（秒）
     */
    int signatureExpire() default -1;

    /**
     * 文件名生成策略
     */
    FilenameStrategy filenameStrategy() default FilenameStrategy.DEFAULT;

    enum StorageType {
        DEFAULT,    // 跟随全局配置
        LOCAL,      // 本地存储
        OSS,        // 阿里云 OSS
        MINIO,      // MinIO
        COS         // 腾讯云 COS
    }

    enum FilenameStrategy {
        DEFAULT,        // 跟随全局配置
        UUID,           // UUID
        TIMESTAMP,      // 时间戳
        ORIGINAL,       // 原始文件名（不安全，不推荐）
        HASH            // 文件内容哈希
    }

    enum Toggle {
        DEFAULT,
        TRUE,
        FALSE
    }
}
