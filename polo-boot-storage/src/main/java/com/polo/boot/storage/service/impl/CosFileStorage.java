package com.polo.boot.storage.service.impl;

import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.storage.model.StoredFileMetadata;
import com.polo.boot.storage.properties.CosProperties;
import com.polo.boot.storage.service.FileStorage;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;

/**
 * 腾讯云 COS 文件存储实现。
 */
@RequiredArgsConstructor
public class CosFileStorage implements FileStorage {

    private final COSClient cosClient;
    private final CosProperties cosProperties;

    @Override
    public String getType() {
        return "cos";
    }

    @Override
    public String upload(InputStream inputStream, long size, String filepath, String contentType) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            if (size >= 0) {
                metadata.setContentLength(size);
            }
            metadata.setContentType(contentType);
            cosClient.putObject(cosProperties.getBucket(), filepath, inputStream, metadata);
            return buildPublicUrl(filepath);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.STORAGE_UPLOAD_ERROR);
        }
    }

    @Override
    public InputStream getInputStream(String filepath) {
        try {
            COSObject cosObject = cosClient.getObject(cosProperties.getBucket(), filepath);
            return cosObject.getObjectContent();
        } catch (Exception ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "获取 COS 文件流失败: " + ex.getMessage());
        }
    }

    @Override
    public InputStream getInputStream(String filepath, long start, long end) {
        try {
            GetObjectRequest request = new GetObjectRequest(cosProperties.getBucket(), filepath);
            request.setRange(start, end);
            COSObject cosObject = cosClient.getObject(request);
            return cosObject.getObjectContent();
        } catch (Exception ex) {
            throw new BizException(ErrorCode.STORAGE_DOWNLOAD_ERROR.getCode(), "获取 COS 范围文件流失败: " + ex.getMessage());
        }
    }

    @Override
    public boolean delete(String filepath) {
        try {
            cosClient.deleteObject(cosProperties.getBucket(), filepath);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public String getBucketName() {
        return cosProperties.getBucket();
    }

    @Override
    public boolean deleteBucket() {
        try {
            cosClient.deleteBucket(cosProperties.getBucket());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public String generateSignedUrl(String filepath, int expireSeconds) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000L);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    cosProperties.getBucket(),
                    filepath,
                    HttpMethodName.GET
            );
            request.setExpiration(expiration);
            URL url = cosClient.generatePresignedUrl(request);
            return url.toString();
        } catch (Exception ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "生成 COS 签名 URL 失败");
        }
    }

    @Override
    public boolean exists(String filepath) {
        try {
            return cosClient.doesObjectExist(cosProperties.getBucket(), filepath);
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public StoredFileMetadata getMetadata(String filepath) {
        try {
            ObjectMetadata metadata = cosClient.getObjectMetadata(cosProperties.getBucket(), filepath);
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

    private String buildPublicUrl(String filepath) {
        if (StringUtils.hasText(cosProperties.getPublicUrl())) {
            return cosProperties.getPublicUrl().replaceAll("/+$", "") + "/" + filepath;
        }
        return "https://" + cosProperties.getBucket() + ".cos." + cosProperties.getRegion() + ".myqcloud.com/" + filepath;
    }

    private String extractFilename(String filepath) {
        int lastSlash = filepath.lastIndexOf('/');
        return lastSlash >= 0 ? filepath.substring(lastSlash + 1) : filepath;
    }
}
