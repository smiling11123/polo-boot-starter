package com.polo.boot.storage.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.storage.model.StoredFileMetadata;
import com.polo.boot.storage.properties.OSSProperties;
import com.polo.boot.storage.service.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;

/**
 * 阿里云 OSS 文件存储实现。
 */
@RequiredArgsConstructor
public class OssFileStorage implements FileStorage {

    private final OSS ossClient;
    private final OSSProperties ossProperties;

    @Override
    public String getType() {
        return "oss";
    }

    @Override
    public String upload(InputStream inputStream, long size, String filepath, String contentType) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            if (size >= 0) {
                metadata.setContentLength(size);
            }
            metadata.setContentType(contentType);
            ossClient.putObject(ossProperties.getBucket(), filepath, inputStream, metadata);
            if (StringUtils.hasText(ossProperties.getPublicUrl())) {
                return ossProperties.getPublicUrl().replaceAll("/+$", "") + "/" + filepath;
            }
            return "https://" + ossProperties.getBucket() + "." + ossProperties.getEndPoint() + "/" + filepath;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.STORAGE_UPLOAD_ERROR);
        }
    }

    @Override
    public InputStream getInputStream(String filepath) {
        try {
            OSSObject ossObject = ossClient.getObject(ossProperties.getBucket(), filepath);
            return ossObject.getObjectContent();
        } catch (Exception ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "获取 OSS 文件流失败: " + ex.getMessage());
        }
    }

    @Override
    public InputStream getInputStream(String filepath, long start, long end) {
        try {
            GetObjectRequest request = new GetObjectRequest(ossProperties.getBucket(), filepath);
            request.setRange(start, end);
            OSSObject ossObject = ossClient.getObject(request);
            return ossObject.getObjectContent();
        } catch (Exception ex) {
            throw new BizException(ErrorCode.STORAGE_DOWNLOAD_ERROR.getCode(), "获取 OSS 范围文件流失败: " + ex.getMessage());
        }
    }

    @Override
    public boolean delete(String filepath) {
        try {
            ossClient.deleteObject(ossProperties.getBucket(), filepath);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public String getBucketName() {
        return ossProperties.getBucket();
    }

    @Override
    public boolean deleteBucket() {
        try {
            ossClient.deleteBucket(ossProperties.getBucket());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public String generateSignedUrl(String filepath, int expireSeconds) {
        Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000L);
        URL url = ossClient.generatePresignedUrl(ossProperties.getBucket(), filepath, expiration);
        return url.toString();
    }

    @Override
    public boolean exists(String filepath) {
        return ossClient.doesObjectExist(ossProperties.getBucket(), filepath);
    }

    @Override
    public StoredFileMetadata getMetadata(String filepath) {
        try {
            ObjectMetadata metadata = ossClient.getObjectMetadata(ossProperties.getBucket(), filepath);
            return StoredFileMetadata.builder()
                    .filepath(filepath)
                    .filename(extractFilename(filepath))
                    .size(metadata.getContentLength())
                    .contentType(StringUtils.hasText(metadata.getContentType()) ? metadata.getContentType() : "application/octet-stream")
                    .storageType(getType())
                    .build();
        } catch (Exception ex) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND.getCode(), "文件不存在");
        }
    }

    private String extractFilename(String filepath) {
        int lastSlash = filepath.lastIndexOf('/');
        return lastSlash >= 0 ? filepath.substring(lastSlash + 1) : filepath;
    }
}
