package com.polo.boot.storage.service.impl;

import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.storage.properties.StorageProperties;
import com.polo.boot.storage.service.FileStorage;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 文件存储工厂，根据配置或显式类型选择底层存储实现。
 */
public class FileStorageFactory {

    private final StorageProperties storageProperties;
    private final Map<StorageProperties.FileType, FileStorage> storageMap = new EnumMap<>(StorageProperties.FileType.class);

    public FileStorageFactory(StorageProperties storageProperties, List<FileStorage> storages) {
        this.storageProperties = storageProperties;
        for (FileStorage storage : storages) {
            StorageProperties.FileType fileType = parseType(storage.getType());
            if (fileType != null) {
                storageMap.put(fileType, storage);
            }
        }
    }

    public FileStorage getStorage() {
        FileStorage storage = storageMap.get(storageProperties.getType());
        if (storage == null) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(),
                    "未找到默认文件存储实现: " + storageProperties.getType().name().toLowerCase(Locale.ROOT));
        }
        return storage;
    }

    public FileStorage getStorage(String type) {
        StorageProperties.FileType fileType = parseType(type);
        if (fileType == null) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "不支持的存储类型: " + type);
        }
        FileStorage storage = storageMap.get(fileType);
        if (storage == null) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "当前未启用存储类型: " + type);
        }
        return storage;
    }

    public String getCurrentType() {
        return storageProperties.getType().name().toLowerCase(Locale.ROOT);
    }

    private StorageProperties.FileType parseType(String type) {
        if (type == null) {
            return null;
        }
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "minio" -> StorageProperties.FileType.MINIO;
            case "oss", "aliyun" -> StorageProperties.FileType.OSS;
            case "cos", "tencent", "qcloud" -> StorageProperties.FileType.COS;
            case "local" -> StorageProperties.FileType.LOCAL;
            case "custom" -> StorageProperties.FileType.CUSTOM;
            default -> null;
        };
    }
}
