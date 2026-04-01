package com.polo.boot.storage.service;

import com.polo.boot.storage.model.ChunkUploadInitRequest;
import com.polo.boot.storage.model.ChunkUploadState;
import com.polo.boot.storage.model.UploadOptions;
import com.polo.boot.storage.model.UploadResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 分片断点续传服务。
 */
public interface ChunkUploadService {

    /**
     * 初始化一个新的分片上传会话。
     */
    ChunkUploadState init(String filename, String originalFilename, String contentType, long totalSize, int totalChunks);

    /**
     * 获取当前上传会话状态。
     */
    ChunkUploadState getState(String uploadId);

    /**
     * 上传单个分片。
     *
     * @param uploadId 上传会话 ID
     * @param chunkIndex 分片索引，从 0 开始
     * @param chunk 当前分片内容
     */
    ChunkUploadState uploadChunk(String uploadId, int chunkIndex, MultipartFile chunk);

    /**
     * 合并全部分片并上传到最终对象存储。
     */
    UploadResult complete(String uploadId, UploadOptions options);

    /**
     * 中止并删除当前上传会话。
     */
    void abort(String uploadId);
}
