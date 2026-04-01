package com.polo.boot.storage.service.impl;

import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.storage.model.StoredFileMetadata;
import com.polo.boot.storage.properties.LocalProperties;
import com.polo.boot.storage.service.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地文件存储实现。
 */
@RequiredArgsConstructor
public class LocalFileStorage implements FileStorage {

    private final LocalProperties localProperties;

    @Override
    public String getType() {
        return "local";
    }

    @Override
    public String upload(InputStream inputStream, long size, String filepath, String contentType) {
        try {
            Path targetPath = resolvePath(filepath);
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return "/files/" + filepath;
        } catch (IOException ex) {
            throw new BizException(ErrorCode.STORAGE_UPLOAD_ERROR);
        }
    }

    @Override
    public InputStream getInputStream(String filepath) {
        try {
            Path path = resolvePath(filepath);
            if (Files.notExists(path)) {
                throw new BizException(ErrorCode.DATA_NOT_FOUND.getCode(), "文件不存在");
            }
            return Files.newInputStream(path);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "获取本地文件流失败: " + ex.getMessage());
        }
    }

    @Override
    public boolean delete(String filepath) {
        try {
            return Files.deleteIfExists(resolvePath(filepath));
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public String generateSignedUrl(String filepath, int expireSeconds) {
        long expiresAt = System.currentTimeMillis() / 1000 + expireSeconds;
        String token = generateToken(filepath, expiresAt);
        return "/files/download?filepath=" + URLEncoder.encode(filepath, StandardCharsets.UTF_8)
                + "&storageType=local&token=" + token + "&expires=" + expiresAt;
    }

    @Override
    public void validateDownloadAccess(String filepath, String token, Long expiresAt) {
        if (!StringUtils.hasText(token) && expiresAt == null) {
            return;
        }
        if (!StringUtils.hasText(token) || expiresAt == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "本地签名下载参数不完整");
        }
        long now = System.currentTimeMillis() / 1000;
        if (expiresAt <= now) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED.getCode(), "下载签名已过期");
        }
        String expected = generateToken(filepath, expiresAt);
        if (!expected.equalsIgnoreCase(token)) {
            throw new BizException(ErrorCode.TOKEN_INVALID.getCode(), "下载签名无效");
        }
    }

    @Override
    public boolean exists(String filepath) {
        return Files.exists(resolvePath(filepath));
    }

    @Override
    public StoredFileMetadata getMetadata(String filepath) {
        Path path = resolvePath(filepath);
        if (Files.notExists(path)) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND.getCode(), "文件不存在");
        }
        try {
            String contentType = Files.probeContentType(path);
            return StoredFileMetadata.builder()
                    .filepath(filepath)
                    .filename(path.getFileName().toString())
                    .size(Files.size(path))
                    .contentType(StringUtils.hasText(contentType) ? contentType : "application/octet-stream")
                    .storageType(getType())
                    .build();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "读取本地文件元信息失败: " + ex.getMessage());
        }
    }

    private String generateToken(String filepath, long expiresAt) {
        if (!StringUtils.hasText(localProperties.getSecurityKey())) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "未配置本地签名下载密钥");
        }
        String data = filepath + ":" + expiresAt;
        return DigestUtils.md5DigestAsHex((data + localProperties.getSecurityKey()).getBytes(StandardCharsets.UTF_8));
    }

    private Path resolvePath(String filepath) {
        if (!StringUtils.hasText(filepath)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "文件路径不能为空");
        }
        Path rootPath = resolveRootPath();
        Path resolved = rootPath.resolve(filepath).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "非法文件路径");
        }
        return resolved;
    }

    private Path resolveRootPath() {
        if (!StringUtils.hasText(localProperties.getLocalPath())) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "未配置本地文件根目录");
        }
        return Paths.get(localProperties.getLocalPath()).toAbsolutePath().normalize();
    }
}
