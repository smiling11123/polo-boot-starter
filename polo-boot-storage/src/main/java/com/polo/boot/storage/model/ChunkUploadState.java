package com.polo.boot.storage.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 分片上传当前状态。
 */
@Data
@Builder
public class ChunkUploadState {
    /**
     * 上传会话 ID。
     */
    private String uploadId;

    /**
     * 原始文件名。
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
     * 当前上传会话状态。
     */
    private ChunkUploadStatus status;

    /**
     * 已上传的分片索引列表。
     */
    private List<Integer> uploadedChunks;

    /**
     * 已上传分片数量。
     */
    private int uploadedCount;

    /**
     * 是否已上传完全部分片，可以执行合并。
     */
    private boolean readyToComplete;
}
