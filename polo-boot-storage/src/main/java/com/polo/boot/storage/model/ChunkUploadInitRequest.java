package com.polo.boot.storage.model;

import lombok.Data;

/**
 * 分片上传初始化请求。
 * 既可以显式用于 init 接口，也可以在首个分片自动初始化时携带元信息。
 */
@Data
public class ChunkUploadInitRequest {
    /**
     * 客户端原始文件名。
     */
    private String originalFilename;

    /**
     * 文件 MIME 类型。
     */
    private String contentType;

    /**
     * 文件总大小，单位字节。
     */
    private long totalSize;

    /**
     * 分片总数。
     */
    private int totalChunks;

    /**
     * 表单字段名，未传时默认使用 file。
     */
    private String fieldName;
}
