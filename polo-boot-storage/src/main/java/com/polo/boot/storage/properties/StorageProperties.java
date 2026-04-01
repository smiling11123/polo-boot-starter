package com.polo.boot.storage.properties;

import com.polo.boot.storage.annotation.UploadFile;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "polo.storage")
public class StorageProperties {
    /**
     * 是否启用存储模块自动装配。
     */
    private boolean enabled = true;

    /**
     * 默认底层存储类型。
     */
    private FileType type = FileType.MINIO;

    /**
     * 上传默认配置。
     */
    private Upload upload = new Upload();

    /**
     * 分片断点续传默认配置。
     */
    private Chunk chunk = new Chunk();

    /**
     * 删除行为默认配置。
     */
    private Delete delete = new Delete();

    /**
     * 文件存储策略
     */
    public enum FileType {
        MINIO,
        OSS,
        COS,
        LOCAL,
        CUSTOM          // 自定义
    }

    @Data
    public static class Upload {
        /**
         * 默认存储路径前缀。
         */
        private String pathPrefix = "uploads";

        /**
         * 默认单文件最大大小（字节）。
         */
        private long maxSize = 10 * 1024 * 1024L;

        /**
         * 默认 MIME 类型白名单。为空表示不限制。
         */
        private List<String> allowedTypes = new ArrayList<>();

        /**
         * 默认扩展名白名单。为空表示不限制。
         */
        private List<String> allowedExtensions = new ArrayList<>();

        /**
         * 是否默认生成缩略图。
         */
        private boolean generateThumbnail = false;

        /**
         * 默认缩略图宽度。
         */
        private int thumbnailWidth = 200;

        /**
         * 默认缩略图高度。
         */
        private int thumbnailHeight = 200;

        /**
         * 是否默认生成私有访问签名链接。
         */
        private boolean privateAccess = false;

        /**
         * 默认签名链接过期秒数。
         */
        private int signatureExpire = 3600;

        /**
         * 默认文件名生成策略。
         */
        private UploadFile.FilenameStrategy filenameStrategy = UploadFile.FilenameStrategy.UUID;
    }

    @Data
    public static class Chunk {
        /**
         * 是否启用分片断点续传服务。
         */
        private boolean enabled = true;

        /**
         * 分片临时目录。
         */
        private String tempDir = System.getProperty("java.io.tmpdir") + "/polo-boot-storage/chunks";

        /**
         * 合并上传成功后是否自动删除本地分片。
         */
        private boolean deleteAfterComplete = true;

        /**
         * 合并分片时的缓冲区大小，单位字节。
         */
        private int mergeBufferSize = 1024 * 1024;
    }

    @Data
    public static class Delete {
        /**
         * 删除原文件时，是否同时尝试删除缩略图。
         */
        private boolean deleteThumbnailOnDelete = false;

        /**
         * 是否允许执行删除第三方存储桶操作。
         * 默认关闭，避免误删整个 bucket。
         */
        private boolean allowBucketDelete = false;
    }
}
