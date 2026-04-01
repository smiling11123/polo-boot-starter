package com.polo.boot.storage.service;

import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.storage.model.StoredFileMetadata;
import com.polo.boot.storage.support.LimitedInputStream;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 底层文件存储接口，只负责对象存储读写。
 */
public interface FileStorage {

    /**
     * 当前存储实现类型，例如 minio、local、oss。
     */
    String getType();

    /**
     * 上传文件内容到指定路径。
     */
    String upload(InputStream inputStream, long size, String filepath, String contentType);

    /**
     * MultipartFile 便捷上传。
     */
    default String upload(MultipartFile multipartFile, String filepath) {
        try (InputStream inputStream = multipartFile.getInputStream()) {
            return upload(inputStream, multipartFile.getSize(), filepath, multipartFile.getContentType());
        } catch (IOException ex) {
            throw new BizException(ErrorCode.STORAGE_UPLOAD_ERROR);
        }
    }

    /**
     * 删除文件。
     */
    boolean delete(String filepath);

    /**
     * 获取当前存储实现绑定的 bucket 名称。
     * 对本地存储等无 bucket 概念的实现，可返回 null。
     */
    default String getBucketName() {
        return null;
    }

    /**
     * 删除当前存储实现绑定的 bucket。
     * 默认不支持，由具体第三方存储实现按需覆盖。
     */
    default boolean deleteBucket() {
        throw new UnsupportedOperationException("当前存储实现不支持删除 bucket");
    }

    /**
     * 生成签名 URL（临时访问）。
     */
    default String generateSignedUrl(String filepath, int expireSeconds) {
        throw new UnsupportedOperationException("当前存储实现不支持签名 URL");
    }

    /**
     * 校验下载访问权限，例如本地签名 URL。
     * 默认实现表示无需额外校验。
     */
    default void validateDownloadAccess(String filepath, String token, Long expiresAt) {
    }

    /**
     * 获取文件输入流（用于下载或读取）。
     */
    InputStream getInputStream(String filepath);

    /**
     * 获取指定字节范围的文件输入流，范围为 [start, end]。
     */
    default InputStream getInputStream(String filepath, long start, long end) {
        if (start < 0 || end < start) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "无效的下载范围");
        }
        try {
            InputStream inputStream = getInputStream(filepath);
            skipFully(inputStream, start);
            return new LimitedInputStream(inputStream, end - start + 1);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.STORAGE_DOWNLOAD_ERROR.getCode(), "读取范围文件流失败");
        }
    }

    /**
     * 获取存储对象元信息。
     */
    StoredFileMetadata getMetadata(String filepath);

    /**
     * 检查文件是否存在。
     */
    boolean exists(String filepath);

    private static void skipFully(InputStream inputStream, long bytesToSkip) throws IOException {
        long remaining = bytesToSkip;
        while (remaining > 0) {
            long skipped = inputStream.skip(remaining);
            if (skipped > 0) {
                remaining -= skipped;
                continue;
            }
            if (inputStream.read() == -1) {
                throw new IOException("文件流长度不足");
            }
            remaining--;
        }
    }
}
