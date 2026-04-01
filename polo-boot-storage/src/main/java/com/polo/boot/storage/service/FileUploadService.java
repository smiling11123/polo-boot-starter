package com.polo.boot.storage.service;

import com.polo.boot.storage.model.OperationResult;
import com.polo.boot.storage.model.StoredFileMetadata;
import com.polo.boot.storage.model.UploadOptions;
import com.polo.boot.storage.model.UploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 高层文件上传服务，负责校验、命名、缩略图、签名 URL 和 UploadResult 组装。
 */
public interface FileUploadService {

    UploadResult upload(MultipartFile file, UploadOptions options);

    List<UploadResult> upload(List<MultipartFile> files, UploadOptions options);

    default UploadResult upload(MultipartFile file, String pathPrefix) {
        return upload(file, UploadOptions.builder().pathPrefix(pathPrefix).build());
    }

    /**
     * 下载文件流（使用默认存储）。
     */
    InputStream download(String filepath);

    /**
     * 下载文件流（指定存储类型）。
     */
    InputStream download(String filepath, String storageType);

    /**
     * 按范围下载文件流，范围为 [start, end]。
     */
    InputStream download(String filepath, String storageType, Long start, Long end);

    /**
     * 获取文件元信息。
     */
    StoredFileMetadata getMetadata(String filepath, String storageType);

    /**
     * 校验下载访问权限，例如本地签名 URL。
     */
    void validateDownloadAccess(String filepath, String storageType, String token, Long expiresAt);

    /**
     * 删除文件并返回详细结果。
     */
    OperationResult delete(String filepath, UploadOptions options);

    /**
     * 删除当前存储实现绑定的 bucket。
     */
    OperationResult deleteBucket(String storageType);

    /**
     * 删除文件（使用默认配置）。
     */
    default OperationResult delete(String filepath) {
        return delete(filepath, null);
    }

    /**
     * 删除 bucket（使用默认存储）。
     */
    default OperationResult deleteBucket() {
        return deleteBucket(null);
    }
}
