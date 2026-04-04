package com.polo.boot.storage.service.impl;

import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.storage.annotation.UploadFile;
import com.polo.boot.storage.context.UploadContext;
import com.polo.boot.storage.model.OperationResult;
import com.polo.boot.storage.model.StoredFileMetadata;
import com.polo.boot.storage.model.UploadOptions;
import com.polo.boot.storage.model.UploadResult;
import com.polo.boot.storage.properties.StorageProperties;
import com.polo.boot.storage.service.FileStorage;
import com.polo.boot.storage.service.FileUploadService;
import com.polo.boot.storage.support.ThumbnailGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@RequiredArgsConstructor
public class DefaultFileUploadService implements FileUploadService {

    private final FileStorageFactory fileStorageFactory;
    private final ThumbnailGenerator thumbnailGenerator;
    private final StorageProperties storageProperties;

    @Override
    public UploadResult upload(MultipartFile file, UploadOptions options) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "上传文件不能为空");
        }
        UploadOptions normalized = normalize(options);
        validateFileType(file, normalized);
        validateFileSize(file, normalized);

        String originalFilename = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "file";
        boolean image = isImage(file);
        boolean stageSource = shouldStageUpload(normalized, image);
        FileStorage storage = resolveStorage(normalized.getStorageType());
        StagedUpload stagedUpload = null;

        try {
            String fileMd5 = null;
            Map<String, Object> metadata;
            String url;

            if (stageSource) {
                stagedUpload = stageUpload(file, image);
                fileMd5 = stagedUpload.fileMd5();
                metadata = stagedUpload.metadata();
            } else {
                metadata = createBaseMetadata(file.getSize(), file.getContentType());
            }

            String filename = generateFilename(originalFilename, normalized.getFilenameStrategy(), fileMd5);
            String filepath = buildFilepath(normalized.getPathPrefix(), filename);

            if (stagedUpload != null) {
                try (InputStream uploadInput = Files.newInputStream(stagedUpload.path(), StandardOpenOption.READ)) {
                    url = storage.upload(uploadInput, stagedUpload.size(), filepath, file.getContentType());
                } catch (IOException ex) {
                    throw new BizException(ErrorCode.STORAGE_UPLOAD_ERROR);
                }
            } else {
                DigestUploadResult digestUploadResult = uploadWithDigest(file, storage, filepath);
                url = digestUploadResult.url();
                fileMd5 = digestUploadResult.fileMd5();
            }

            UploadResult.UploadResultBuilder resultBuilder = UploadResult.builder()
                    .uploadId(UUID.randomUUID().toString())
                    .fieldName(normalized.getFieldName())
                    .originalFilename(originalFilename)
                    .filename(filename)
                    .filepath(filepath)
                    .url(url)
                    .size(file.getSize())
                    .contentType(file.getContentType())
                    .fileMd5(fileMd5)
                    .storageType(storage.getType())
                    .metadata(metadata);

            if (Boolean.TRUE.equals(normalized.getGenerateThumbnail()) && image && stagedUpload != null) {
                try (InputStream thumbnailSource = Files.newInputStream(stagedUpload.path(), StandardOpenOption.READ);
                     InputStream thumbnailInput = thumbnailGenerator.generate(
                             thumbnailSource,
                             normalized.getThumbnailWidth(),
                             normalized.getThumbnailHeight()
                     )) {
                    String thumbPath = buildFilepath(normalized.getPathPrefix(), "thumbs/" + filename + ".jpg");
                    String thumbUrl = storage.upload(thumbnailInput, -1L, thumbPath, "image/jpeg");
                    resultBuilder.thumbnailUrl(thumbUrl)
                            .thumbnailWidth(normalized.getThumbnailWidth())
                            .thumbnailHeight(normalized.getThumbnailHeight());
                } catch (IOException ex) {
                    throw new BizException(ErrorCode.STORAGE_UPLOAD_ERROR);
                }
            }

            if (Boolean.TRUE.equals(normalized.getPrivateAccess())) {
                resultBuilder.signedUrl(storage.generateSignedUrl(filepath, normalized.getSignatureExpire()))
                        .signedUrlExpire(normalized.getSignatureExpire());
            }

            UploadResult uploadResult = resultBuilder.build();
            UploadContext.add(normalized.getFieldName(), List.of(uploadResult));
            return uploadResult;
        } finally {
            deleteTempFile(stagedUpload);
        }
    }

    @Override
    public List<UploadResult> upload(List<MultipartFile> files, UploadOptions options) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<UploadResult> results = new ArrayList<>(files.size());
        for (MultipartFile file : files) {
            results.add(upload(file, options));
        }
        return results;
    }

    @Override
    public InputStream download(String filepath) {
        return download(filepath, null);
    }

    @Override
    public InputStream download(String filepath, String storageType) {
        return download(filepath, storageType, null, null);
    }

    @Override
    public InputStream download(String filepath, String storageType, Long start, Long end) {
        return download(filepath, storageType, null, start, end);
    }

    @Override
    public InputStream download(String filepath, String storageType, StoredFileMetadata metadata, Long start, Long end) {
        if (!StringUtils.hasText(filepath)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "文件路径不能为空");
        }
        FileStorage storage = resolveStorage(storageType);
        if (start == null && end == null) {
            return storage.getInputStream(filepath);
        }
        StoredFileMetadata effectiveMetadata = metadata != null ? metadata : storage.getMetadata(filepath);
        long actualStart = start == null ? 0L : start;
        long actualEnd = end == null ? effectiveMetadata.getSize() - 1 : end;
        validateRange(actualStart, actualEnd, effectiveMetadata.getSize());
        return storage.getInputStream(filepath, actualStart, actualEnd);
    }

    @Override
    public StoredFileMetadata getMetadata(String filepath, String storageType) {
        if (!StringUtils.hasText(filepath)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "文件路径不能为空");
        }
        FileStorage storage = resolveStorage(storageType);
        return storage.getMetadata(filepath);
    }

    @Override
    public void validateDownloadAccess(String filepath, String storageType, String token, Long expiresAt) {
        if (!StringUtils.hasText(filepath)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "文件路径不能为空");
        }
        FileStorage storage = resolveStorage(storageType);
        storage.validateDownloadAccess(filepath, token, expiresAt);
    }

    @Override
    public OperationResult delete(String filepath, UploadOptions options) {
        if (!StringUtils.hasText(filepath)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "文件路径不能为空");
        }
        UploadOptions normalized = normalize(options);
        FileStorage storage = resolveStorage(normalized.getStorageType());

        try {
            StoredFileMetadata originalMetadata = storage.getMetadata(filepath);
            boolean success = storage.delete(filepath);
            if (!success) {
                throw new BizException(ErrorCode.STORAGE_DELETE_ERROR.getCode(), "删除原文件失败");
            }
            String thumbnailPath = buildThumbnailPath(filepath);
            boolean thumbnailDeleted = false;
            boolean thumbnailExisted = false;
            boolean deleteThumbnailOnDelete = storageProperties.getDelete().isDeleteThumbnailOnDelete();
            if (deleteThumbnailOnDelete) {
                thumbnailExisted = storage.exists(thumbnailPath);
                if (thumbnailExisted) {
                    thumbnailDeleted = storage.delete(thumbnailPath);
                }
            }

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("success", true);
            metadata.put("deleteThumbnailOnDelete", deleteThumbnailOnDelete);
            metadata.put("thumbnailPath", thumbnailPath);
            metadata.put("thumbnailExisted", thumbnailExisted);
            metadata.put("thumbnailDeleted", thumbnailDeleted);

            return OperationResult.builder()
                    .operationId(UUID.randomUUID().toString())
                    .operationType(OperationResult.OperationType.DELETE)
                    .filename(originalMetadata.getFilename())
                    .originalFilename(originalMetadata.getFilename())
                    .originalFilepath(filepath)
                    .size(originalMetadata.getSize())
                    .contentType(originalMetadata.getContentType())
                    .storageType(storage.getType())
                    .metadata(metadata)
                    .build();

        } catch (BizException ex) {
            throw ex;
        } catch (Exception e) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "文件删除失败: " + e.getMessage());
        }
    }

    @Override
    public OperationResult deleteBucket(String storageType) {
        if (!storageProperties.getDelete().isAllowBucketDelete()) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "当前未开启删除存储桶能力");
        }
        FileStorage storage = resolveStorage(storageType);
        String bucketName = storage.getBucketName();
        try {
            boolean success = storage.deleteBucket();
            if (!success) {
                throw new BizException(ErrorCode.STORAGE_DELETE_ERROR.getCode(), "删除存储桶失败");
            }

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("success", true);
            metadata.put("bucketName", bucketName);
            metadata.put("storageType", storage.getType());

            return OperationResult.builder()
                    .operationId(UUID.randomUUID().toString())
                    .operationType(OperationResult.OperationType.DELETE_BUCKET)
                    .filename(bucketName)
                    .originalFilename(bucketName)
                    .originalFilepath(bucketName)
                    .storageType(storage.getType())
                    .metadata(metadata)
                    .build();
        } catch (UnsupportedOperationException ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "当前存储实现不支持删除 bucket");
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "删除存储桶失败: " + ex.getMessage());
        }
    }

    private UploadOptions normalize(UploadOptions options) {
        UploadOptions normalized = options == null ? UploadOptions.builder().build() : options;
        StorageProperties.Upload defaults = storageProperties.getUpload();
        if (!StringUtils.hasText(normalized.getFieldName())) {
            normalized.setFieldName("file");
        }
        if (!StringUtils.hasText(normalized.getPathPrefix())) {
            normalized.setPathPrefix(defaults.getPathPrefix());
        }
        if (normalized.getMaxSize() == null) {
            normalized.setMaxSize(defaults.getMaxSize() == null ? null : defaults.getMaxSize().toBytes());
        }
        if (normalized.getAllowedTypes() == null && defaults.getAllowedTypes() != null && !defaults.getAllowedTypes().isEmpty()) {
            normalized.setAllowedTypes(defaults.getAllowedTypes().toArray(String[]::new));
        }
        if (normalized.getAllowedExtensions() == null && defaults.getAllowedExtensions() != null && !defaults.getAllowedExtensions().isEmpty()) {
            normalized.setAllowedExtensions(defaults.getAllowedExtensions().toArray(String[]::new));
        }
        if (normalized.getGenerateThumbnail() == null) {
            normalized.setGenerateThumbnail(defaults.isGenerateThumbnail());
        }
        if (normalized.getThumbnailWidth() == null) {
            normalized.setThumbnailWidth(defaults.getThumbnailWidth());
        }
        if (normalized.getThumbnailHeight() == null) {
            normalized.setThumbnailHeight(defaults.getThumbnailHeight());
        }
        if (!StringUtils.hasText(normalized.getStorageType())) {
            normalized.setStorageType(storageProperties.getType().name().toLowerCase());
        }
        if (normalized.getPrivateAccess() == null) {
            normalized.setPrivateAccess(defaults.isPrivateAccess());
        }
        if (normalized.getSignatureExpire() == null) {
            normalized.setSignatureExpire(defaults.getSignatureExpire());
        }
        if (normalized.getFilenameStrategy() == null) {
            normalized.setFilenameStrategy(defaults.getFilenameStrategy());
        }
        return normalized;
    }

    private FileStorage resolveStorage(String storageType) {
        if (StringUtils.hasText(storageType)) {
            return fileStorageFactory.getStorage(storageType);
        }
        return fileStorageFactory.getStorage();
    }

    private void validateFileType(MultipartFile file, UploadOptions options) {
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        if (options.getAllowedExtensions() != null && options.getAllowedExtensions().length > 0) {
            String extension = getExtension(originalFilename).toLowerCase();
            boolean allowed = Arrays.stream(options.getAllowedExtensions())
                    .map(String::toLowerCase)
                    .anyMatch(extension::equals);
            if (!allowed) {
                throw new BizException(ErrorCode.PARAM_ERROR.getCode(),
                        "不支持的文件扩展名: ." + extension + "，仅允许: " + String.join(", ", options.getAllowedExtensions()));
            }
        }

        if (options.getAllowedTypes() != null && options.getAllowedTypes().length > 0) {
            boolean allowed = Arrays.stream(options.getAllowedTypes())
                    .anyMatch(type -> type.equalsIgnoreCase(contentType));
            if (!allowed) {
                throw new BizException(ErrorCode.PARAM_ERROR.getCode(),
                        "不支持的文件类型: " + contentType + "，仅允许: " + String.join(", ", options.getAllowedTypes()));
            }
        }
    }

    private void validateFileSize(MultipartFile file, UploadOptions options) {
        if (options.getMaxSize() != null && options.getMaxSize() > 0 && file.getSize() > options.getMaxSize()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(),
                    "文件大小超过限制，当前: " + formatSize(file.getSize()) + "，最大允许: " + formatSize(options.getMaxSize()));
        }
    }

    private String generateFilename(String originalFilename,
                                    UploadFile.FilenameStrategy strategy,
                                    String precomputedMd5) {
        String extension = getExtension(originalFilename);
        String basename = switch (strategy) {
            case DEFAULT -> UUID.randomUUID().toString().replace("-", "");
            case UUID -> UUID.randomUUID().toString().replace("-", "");
            case TIMESTAMP -> System.currentTimeMillis() + "_" + new Random().nextInt(1000);
            case HASH -> {
                if (!StringUtils.hasText(precomputedMd5)) {
                    throw new BizException(ErrorCode.STORAGE_UPLOAD_ERROR.getCode(), "HASH 文件名策略缺少文件摘要");
                }
                yield precomputedMd5;
            }
            case ORIGINAL -> sanitizeBaseName(stripExtension(originalFilename));
        };
        return StringUtils.hasText(extension) ? basename + "." + extension.toLowerCase() : basename;
    }

    private boolean isImage(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    private Map<String, Object> createBaseMetadata(long size, String contentType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("size", size);
        metadata.put("contentType", contentType);
        return metadata;
    }

    private Map<String, Object> extractImageMetadata(Path path, Map<String, Object> metadata) {
        Map<String, Object> result = metadata == null ? new HashMap<>() : metadata;
        try (InputStream inputStream = Files.newInputStream(path, StandardOpenOption.READ)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image != null) {
                result.put("width", image.getWidth());
                result.put("height", image.getHeight());
            }
        } catch (IOException ignored) {
            // 图片元数据提取失败不影响主上传流程
        }
        return result;
    }

    private boolean shouldStageUpload(UploadOptions options, boolean image) {
        return image
                || Boolean.TRUE.equals(options.getGenerateThumbnail())
                || options.getFilenameStrategy() == UploadFile.FilenameStrategy.HASH;
    }

    private StagedUpload stageUpload(MultipartFile file, boolean image) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("polo-upload-", ".tmp");
            MessageDigest digest = md5Digest();
            try (InputStream inputStream = file.getInputStream();
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest);
                 OutputStream outputStream = Files.newOutputStream(
                         tempFile,
                         StandardOpenOption.WRITE,
                         StandardOpenOption.TRUNCATE_EXISTING
                 )) {
                digestInputStream.transferTo(outputStream);
            }
            String md5 = toHex(digest.digest());
            Map<String, Object> metadata = createBaseMetadata(file.getSize(), file.getContentType());
            if (image) {
                metadata = extractImageMetadata(tempFile, metadata);
            }
            return new StagedUpload(tempFile, file.getSize(), md5, metadata);
        } catch (IOException ex) {
            deleteTempFile(tempFile);
            throw new BizException(ErrorCode.STORAGE_UPLOAD_ERROR);
        }
    }

    private DigestUploadResult uploadWithDigest(MultipartFile file, FileStorage storage, String filepath) {
        try {
            MessageDigest digest = md5Digest();
            try (InputStream inputStream = file.getInputStream();
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                String url = storage.upload(digestInputStream, file.getSize(), filepath, file.getContentType());
                return new DigestUploadResult(url, toHex(digest.digest()));
            }
        } catch (IOException ex) {
            throw new BizException(ErrorCode.STORAGE_UPLOAD_ERROR);
        }
    }

    private MessageDigest md5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前运行环境不支持 MD5", ex);
        }
    }

    private String toHex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    private void deleteTempFile(StagedUpload stagedUpload) {
        if (stagedUpload != null) {
            deleteTempFile(stagedUpload.path());
        }
    }

    private void deleteTempFile(Path tempFile) {
        if (tempFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException ignored) {
        }
    }

    private record StagedUpload(Path path, long size, String fileMd5, Map<String, Object> metadata) {
    }

    private record DigestUploadResult(String url, String fileMd5) {
    }

    private String buildFilepath(String pathPrefix, String filename) {
        String safePathPrefix = pathPrefix.replace("\\", "/").replaceAll("^/+", "").replaceAll("/+$", "");
        return StringUtils.hasText(safePathPrefix) ? safePathPrefix + "/" + filename : filename;
    }

    private String getExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String stripExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return StringUtils.hasText(filename) ? filename : "file";
        }
        return filename.substring(0, filename.lastIndexOf('.'));
    }

    private String sanitizeBaseName(String baseName) {
        String sanitized = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return StringUtils.hasText(sanitized) ? sanitized : "file";
    }

    private void validateRange(long start, long end, long totalSize) {
        if (start < 0 || end < start || start >= totalSize || end >= totalSize) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "无效的下载范围");
        }
    }

    private String buildThumbnailPath(String filepath) {
        String normalizedPath = filepath.replace("\\", "/");
        int lastSlash = normalizedPath.lastIndexOf('/');
        String directory = lastSlash >= 0 ? normalizedPath.substring(0, lastSlash) : "";
        String filename = lastSlash >= 0 ? normalizedPath.substring(lastSlash + 1) : normalizedPath;
        return StringUtils.hasText(directory) ? directory + "/thumbs/" + filename + ".jpg" : "thumbs/" + filename + ".jpg";
    }

    private String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        }
        return String.format("%.2f MB", size / (1024.0 * 1024));
    }
}
