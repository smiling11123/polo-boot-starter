package com.polo.boot.storage.service.impl;

import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.storage.model.StoredFileMetadata;
import com.polo.boot.storage.properties.MinioProperties;
import com.polo.boot.storage.service.FileStorage;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * MinIO 文件存储实现。
 */
@RequiredArgsConstructor
public class MinioFileStorage implements FileStorage {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public String getType() {
        return "minio";
    }

    @Override
    public String upload(InputStream inputStream, long size, String filepath, String contentType) {
        try {
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(filepath)
                    .contentType(contentType);
            if (size >= 0) {
                builder.stream(inputStream, size, -1);
            } else {
                builder.stream(inputStream, -1, 10 * 1024 * 1024);
            }
            minioClient.putObject(
                    builder.build()
            );
            if (StringUtils.hasText(minioProperties.getPublicUrl())) {
                return buildPublicUrl(filepath);
            }
            return minioProperties.getEndpoint().replaceAll("/+$", "")
                    + "/" + minioProperties.getBucket() + "/" + filepath;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.STORAGE_UPLOAD_ERROR);
        }
    }

    @Override
    public InputStream getInputStream(String filepath) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(filepath)
                            .build()
            );
        } catch (Exception ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "获取 MinIO 文件流失败: " + ex.getMessage());
        }
    }

    @Override
    public InputStream getInputStream(String filepath, long start, long end) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(filepath)
                            .offset(start)
                            .length(end - start + 1)
                            .build()
            );
        } catch (Exception ex) {
            throw new BizException(ErrorCode.STORAGE_DOWNLOAD_ERROR.getCode(), "获取 MinIO 范围文件流失败: " + ex.getMessage());
        }
    }

    @Override
    public boolean delete(String filepath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(filepath)
                    .build());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public String getBucketName() {
        return minioProperties.getBucket();
    }

    @Override
    public boolean deleteBucket() {
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .build());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public String generateSignedUrl(String filepath, int expireSeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucket())
                            .object(filepath)
                            .expiry(expireSeconds)
                            .build()
            );
        } catch (Exception ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "生成签名 URL 失败");
        }
    }

    @Override
    public boolean exists(String filepath) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(filepath)
                    .build());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public StoredFileMetadata getMetadata(String filepath) {
        try {
            var stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(filepath)
                    .build());
            return StoredFileMetadata.builder()
                    .filepath(filepath)
                    .filename(extractFilename(filepath))
                    .size(stat.size())
                    .contentType(StringUtils.hasText(stat.contentType()) ? stat.contentType() : "application/octet-stream")
                    .storageType(getType())
                    .build();
        } catch (Exception ex) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND.getCode(), "文件不存在");
        }
    }

    private String buildPublicUrl(String filepath) {
        String base = minioProperties.getPublicUrl().replaceAll("/+$", "");
        if (base.endsWith("/" + minioProperties.getBucket()) || base.contains("://" + minioProperties.getBucket() + ".")) {
            return base + "/" + filepath;
        }
        return base + "/" + minioProperties.getBucket() + "/" + filepath;
    }

    private String extractFilename(String filepath) {
        int lastSlash = filepath.lastIndexOf('/');
        return lastSlash >= 0 ? filepath.substring(lastSlash + 1) : filepath;
    }
}
