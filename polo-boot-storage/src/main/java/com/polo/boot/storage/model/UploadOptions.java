package com.polo.boot.storage.model;

import com.polo.boot.storage.annotation.UploadFile;
import lombok.Builder;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
@Builder
public class UploadOptions {

    /**
     * 当前上传表单字段名，例如 file、avatar、attachments。
     */
    private String fieldName;

    /**
     * 本次上传允许的 MIME 类型白名单。
     * 为空时回退到全局默认配置。
     */
    private String[] allowedTypes;

    /**
     * 本次上传允许的文件扩展名白名单。
     * 为空时回退到全局默认配置。
     */
    private String[] allowedExtensions;

    /**
     * 本次上传允许的最大文件大小，单位字节。
     * 为空时回退到全局默认配置。
     */
    private Long maxSize;

    /**
     * 是否生成缩略图。
     * 为空时回退到全局默认配置。
     */
    private Boolean generateThumbnail;

    /**
     * 缩略图宽度。
     * 为空时回退到全局默认配置。
     */
    private Integer thumbnailWidth;

    /**
     * 缩略图高度。
     * 为空时回退到全局默认配置。
     */
    private Integer thumbnailHeight;

    /**
     * 指定本次上传使用的底层存储类型，例如 minio、oss、local。
     * 为空时回退到全局默认配置。
     */
    private String storageType;

    /**
     * 上传路径前缀。
     * 为空时回退到全局默认配置。
     */
    private String pathPrefix;

    /**
     * 是否使用私有访问模式并生成签名链接。
     * 为空时回退到全局默认配置。
     */
    private Boolean privateAccess;

    /**
     * 签名链接过期时间，单位秒。
     * 为空时回退到全局默认配置。
     */
    private Integer signatureExpire;

    /**
     * 文件名生成策略。
     * 为空时回退到全局默认配置。
     */
    private UploadFile.FilenameStrategy filenameStrategy;

    public static UploadOptions from(UploadFile annotation, String fieldName) {
        return UploadOptions.builder()
                .fieldName(StringUtils.hasText(fieldName) ? fieldName : "file")
                .allowedTypes(annotation.allowedTypes().length == 0 ? null : annotation.allowedTypes())
                .allowedExtensions(annotation.allowedExtensions().length == 0 ? null : annotation.allowedExtensions())
                .maxSize(annotation.maxSize() > 0 ? annotation.maxSize() : null)
                .generateThumbnail(resolveToggle(annotation.generateThumbnail()))
                .thumbnailWidth(annotation.thumbnailWidth() > 0 ? annotation.thumbnailWidth() : null)
                .thumbnailHeight(annotation.thumbnailHeight() > 0 ? annotation.thumbnailHeight() : null)
                .storageType(annotation.storage() == UploadFile.StorageType.DEFAULT ? null : annotation.storage().name().toLowerCase())
                .pathPrefix(StringUtils.hasText(annotation.pathPrefix()) ? annotation.pathPrefix() : null)
                .privateAccess(resolveToggle(annotation.privateAccess()))
                .signatureExpire(annotation.signatureExpire() > 0 ? annotation.signatureExpire() : null)
                .filenameStrategy(annotation.filenameStrategy() == UploadFile.FilenameStrategy.DEFAULT ? null : annotation.filenameStrategy())
                .build();
    }

    private static Boolean resolveToggle(UploadFile.Toggle toggle) {
        if (toggle == null || toggle == UploadFile.Toggle.DEFAULT) {
            return null;
        }
        return toggle == UploadFile.Toggle.TRUE;
    }
}
