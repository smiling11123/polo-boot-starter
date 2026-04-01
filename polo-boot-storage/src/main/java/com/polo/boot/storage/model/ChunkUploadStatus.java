package com.polo.boot.storage.model;

/**
 * 分片上传会话状态。
 */
public enum ChunkUploadStatus {
    /**
     * 已初始化，尚未开始上传分片。
     */
    INIT,

    /**
     * 正在上传分片。
     */
    UPLOADING,

    /**
     * 正在合并分片并上传最终文件。
     */
    COMPLETING,

    /**
     * 已完成上传。
     */
    COMPLETED,

    /**
     * 已取消上传。
     */
    ABORTED
}
