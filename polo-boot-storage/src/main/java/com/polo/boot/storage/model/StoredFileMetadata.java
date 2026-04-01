package com.polo.boot.storage.model;

import lombok.Builder;
import lombok.Data;

/**
 * 底层存储对象的元信息。
 */
@Data
@Builder
public class StoredFileMetadata {
    /**
     * 存储中的相对路径。
     */
    private String filepath;

    /**
     * 文件名。
     */
    private String filename;

    /**
     * 文件大小，单位字节。
     */
    private long size;

    /**
     * 文件 MIME 类型。
     */
    private String contentType;

    /**
     * 底层存储类型。
     */
    private String storageType;
}
