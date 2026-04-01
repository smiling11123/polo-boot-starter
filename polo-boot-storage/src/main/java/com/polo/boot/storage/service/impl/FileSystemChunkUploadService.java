package com.polo.boot.storage.service.impl;

import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.storage.model.ChunkUploadInitRequest;
import com.polo.boot.storage.model.ChunkUploadState;
import com.polo.boot.storage.model.ChunkUploadStatus;
import com.polo.boot.storage.model.UploadOptions;
import com.polo.boot.storage.model.UploadResult;
import com.polo.boot.storage.properties.StorageProperties;
import com.polo.boot.storage.service.ChunkUploadService;
import com.polo.boot.storage.service.FileUploadService;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 基于本地临时分片目录的通用断点续传实现。
 * 同一个 uploadId 下的分片上传、合并操作串行执行，不同 uploadId 之间可以并发。
 */
public class FileSystemChunkUploadService implements ChunkUploadService {

    private static final String META_FILE = "meta.properties";
    private static final String CHUNKS_DIR = "chunks";
    private static final String MERGED_FILE = "merged.bin";
    private static final String TEMP_SUFFIX = ".tmp";

    private final FileUploadService fileUploadService;
    private final StorageProperties storageProperties;
    private final ConcurrentMap<String, SessionControl> sessionControls = new ConcurrentHashMap<>();

    public FileSystemChunkUploadService(FileUploadService fileUploadService, StorageProperties storageProperties) {
        this.fileUploadService = fileUploadService;
        this.storageProperties = storageProperties;
    }

    @Override
    public ChunkUploadState init(String filename, String originalFilename, String contentType, long totalSize, int totalChunks) {
        ChunkUploadInitRequest request = new ChunkUploadInitRequest();
        request.setFieldName(filename);
        request.setOriginalFilename(originalFilename);
        request.setContentType(contentType);
        request.setTotalSize(totalSize);
        request.setTotalChunks(totalChunks);

        validateInitRequest(request);
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        Path sessionDir = sessionDir(uploadId);
        try {
            Files.createDirectories(sessionDir.resolve(CHUNKS_DIR));
            ChunkMeta meta = new ChunkMeta(
                    uploadId,
                    StringUtils.hasText(request.getFieldName()) ? request.getFieldName() : "file",
                    request.getOriginalFilename(),
                    request.getContentType(),
                    request.getTotalSize(),
                    request.getTotalChunks(),
                    ChunkUploadStatus.INIT
            );
            saveMeta(meta);
            syncControl(meta);
            return buildState(meta);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "初始化分片上传会话失败");
        }
    }

    @Override
    public ChunkUploadState getState(String uploadId) {
        ChunkMeta meta = loadMeta(uploadId);
        syncControl(meta);
        return buildState(meta);
    }

    @Override
    public ChunkUploadState uploadChunk(String uploadId, int chunkIndex, MultipartFile chunk) {
        if (chunk == null || chunk.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "分片文件不能为空");
        }
        ChunkMeta meta = loadMeta(uploadId);
        SessionControl control = syncControl(meta);
        control.executionLock().lock();
        try {
            ChunkMeta latestMeta = loadMeta(uploadId);
            updateControlStatus(control, latestMeta.status());
            ensureCanUpload(control.status().get());
            if (chunkIndex < 0 || chunkIndex >= latestMeta.totalChunks()) {
                throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "分片索引超出范围");
            }

            if (latestMeta.status() == ChunkUploadStatus.INIT) {
                latestMeta = latestMeta.withStatus(ChunkUploadStatus.UPLOADING);
                saveMeta(latestMeta);
                updateControlStatus(control, latestMeta.status());
            }

            writeChunk(uploadId, chunkIndex, chunk, control);
            ChunkMeta currentMeta = loadMeta(uploadId);
            updateControlStatus(control, currentMeta.status());
            return buildState(currentMeta);
        } finally {
            control.executionLock().unlock();
            cleanupIfNeeded(uploadId, control);
        }
    }

    @Override
    public UploadResult complete(String uploadId, UploadOptions options) {
        ChunkMeta meta = loadMeta(uploadId);
        SessionControl control = syncControl(meta);
        control.executionLock().lock();
        try {
            ChunkMeta latestMeta = loadMeta(uploadId);
            updateControlStatus(control, latestMeta.status());
            ensureCanComplete(control.status().get());

            List<Integer> uploadedChunks = listUploadedChunks(uploadId);
            if (uploadedChunks.size() != latestMeta.totalChunks()) {
                throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "分片未上传完整，无法合并");
            }

            ChunkMeta completingMeta = latestMeta.withStatus(ChunkUploadStatus.COMPLETING);
            saveMeta(completingMeta);
            updateControlStatus(control, ChunkUploadStatus.COMPLETING);

            Path mergedFile = sessionDir(uploadId).resolve(MERGED_FILE);
            mergeChunks(completingMeta, mergedFile, control);
            ensureNotAborted(control);
            validateMergedFile(completingMeta, mergedFile);

            UploadOptions finalOptions = options != null ? options : UploadOptions.builder().build();
            if (!StringUtils.hasText(finalOptions.getFieldName())) {
                finalOptions.setFieldName(completingMeta.fieldName());
            }

            UploadResult result = fileUploadService.upload(
                    new LocalChunkMultipartFile(completingMeta.fieldName(), completingMeta.originalFilename(), completingMeta.contentType(), mergedFile),
                    finalOptions
            );

            if (control.status().get() == ChunkUploadStatus.ABORTED) {
                throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "上传已取消");
            }

            if (storageProperties.getChunk().isDeleteAfterComplete()) {
                deleteRecursively(sessionDir(uploadId));
                sessionControls.remove(uploadId);
            } else {
                ChunkMeta completedMeta = completingMeta.withStatus(ChunkUploadStatus.COMPLETED);
                saveMeta(completedMeta);
                updateControlStatus(control, ChunkUploadStatus.COMPLETED);
            }
            return result;
        } finally {
            if (control.executionLock().isHeldByCurrentThread()) {
                control.executionLock().unlock();
            }
            cleanupIfNeeded(uploadId, control);
        }
    }

    @Override
    public void abort(String uploadId) {
        ChunkMeta meta = loadMeta(uploadId);
        SessionControl control = syncControl(meta);
        updateControlStatus(control, ChunkUploadStatus.ABORTED);
        saveMeta(meta.withStatus(ChunkUploadStatus.ABORTED));

        if (control.executionLock().tryLock()) {
            try {
                deleteRecursively(sessionDir(uploadId));
                sessionControls.remove(uploadId);
            } finally {
                control.executionLock().unlock();
            }
        }
    }

    private void validateInitRequest(ChunkUploadInitRequest request) {
        if (request == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "初始化参数不能为空");
        }
        if (!StringUtils.hasText(request.getOriginalFilename())) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "原始文件名不能为空");
        }
        if (request.getTotalSize() <= 0) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "文件总大小必须大于 0");
        }
        if (request.getTotalChunks() <= 0) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "分片总数必须大于 0");
        }
    }

    private void writeChunk(String uploadId, int chunkIndex, MultipartFile chunk, SessionControl control) {
        Path target = chunkPath(uploadId, chunkIndex);
        Path temp = target.resolveSibling(target.getFileName() + TEMP_SUFFIX);
        try {
            Files.createDirectories(target.getParent());
            byte[] buffer = new byte[Math.max(storageProperties.getChunk().getMergeBufferSize(), 8 * 1024)];
            try (InputStream inputStream = chunk.getInputStream();
                 BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                 BufferedOutputStream outputStream = new BufferedOutputStream(
                         Files.newOutputStream(temp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                int len;
                while ((len = bufferedInputStream.read(buffer)) != -1) {
                    ensureNotAborted(control);
                    outputStream.write(buffer, 0, len);
                }
            }
            moveAtomically(temp, target);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.STORAGE_UPLOAD_ERROR.getCode(), "保存分片失败");
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
            }
        }
    }

    private void mergeChunks(ChunkMeta meta, Path mergedFile, SessionControl control) {
        try {
            Files.createDirectories(mergedFile.getParent());
            try (BufferedOutputStream outputStream = new BufferedOutputStream(
                    Files.newOutputStream(mergedFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                byte[] buffer = new byte[Math.max(storageProperties.getChunk().getMergeBufferSize(), 8 * 1024)];
                for (int index = 0; index < meta.totalChunks(); index++) {
                    ensureNotAborted(control);
                    Path partFile = chunkPath(meta.uploadId(), index);
                    try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(partFile))) {
                        int len;
                        while ((len = inputStream.read(buffer)) != -1) {
                            ensureNotAborted(control);
                            outputStream.write(buffer, 0, len);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "合并分片失败");
        }
    }

    private ChunkUploadState buildState(ChunkMeta meta) {
        List<Integer> uploadedChunks = listUploadedChunks(meta.uploadId());
        return ChunkUploadState.builder()
                .uploadId(meta.uploadId())
                .originalFilename(meta.originalFilename())
                .contentType(meta.contentType())
                .totalSize(meta.totalSize())
                .totalChunks(meta.totalChunks())
                .status(meta.status())
                .uploadedChunks(uploadedChunks)
                .uploadedCount(uploadedChunks.size())
                .readyToComplete(meta.status() != ChunkUploadStatus.ABORTED
                        && meta.status() != ChunkUploadStatus.COMPLETING
                        && meta.status() != ChunkUploadStatus.COMPLETED
                        && uploadedChunks.size() == meta.totalChunks())
                .build();
    }

    private List<Integer> listUploadedChunks(String uploadId) {
        Path chunksDir = sessionDir(uploadId).resolve(CHUNKS_DIR);
        if (Files.notExists(chunksDir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(chunksDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".part"))
                    .map(name -> name.substring(0, name.length() - 5))
                    .map(Integer::parseInt)
                    .sorted()
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "读取分片状态失败");
        }
    }

    private ChunkMeta loadMeta(String uploadId) {
        Path metaFile = sessionDir(uploadId).resolve(META_FILE);
        if (Files.notExists(metaFile)) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND.getCode(), "分片上传会话不存在");
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(metaFile)) {
            properties.load(inputStream);
            return new ChunkMeta(
                    properties.getProperty("uploadId"),
                    properties.getProperty("fieldName"),
                    properties.getProperty("originalFilename"),
                    properties.getProperty("contentType"),
                    Long.parseLong(properties.getProperty("totalSize")),
                    Integer.parseInt(properties.getProperty("totalChunks")),
                    ChunkUploadStatus.valueOf(properties.getProperty("status", ChunkUploadStatus.INIT.name()))
            );
        } catch (IOException ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "读取分片元数据失败");
        }
    }

    private void saveMeta(ChunkMeta meta) {
        Properties properties = new Properties();
        properties.setProperty("uploadId", meta.uploadId());
        properties.setProperty("fieldName", meta.fieldName());
        properties.setProperty("originalFilename", meta.originalFilename());
        properties.setProperty("contentType", meta.contentType() == null ? "" : meta.contentType());
        properties.setProperty("totalSize", String.valueOf(meta.totalSize()));
        properties.setProperty("totalChunks", String.valueOf(meta.totalChunks()));
        properties.setProperty("status", meta.status().name());
        Path metaFile = sessionDir(meta.uploadId()).resolve(META_FILE);
        try (BufferedOutputStream outputStream = new BufferedOutputStream(
                Files.newOutputStream(metaFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            properties.store(outputStream, "chunk upload meta");
        } catch (IOException ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "保存分片元数据失败");
        }
    }

    private SessionControl syncControl(ChunkMeta meta) {
        return sessionControls.compute(meta.uploadId(), (uploadId, existing) -> {
            if (existing == null) {
                return new SessionControl(meta.status());
            }
            existing.status().set(meta.status());
            return existing;
        });
    }

    private void updateControlStatus(SessionControl control, ChunkUploadStatus status) {
        control.status().set(status);
    }

    private void ensureCanUpload(ChunkUploadStatus status) {
        if (status == ChunkUploadStatus.ABORTED) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "上传已取消");
        }
        if (status == ChunkUploadStatus.COMPLETING) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "正在合并分片，暂不允许继续上传");
        }
        if (status == ChunkUploadStatus.COMPLETED) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "上传已完成");
        }
    }

    private void ensureCanComplete(ChunkUploadStatus status) {
        if (status == ChunkUploadStatus.ABORTED) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "上传已取消");
        }
        if (status == ChunkUploadStatus.COMPLETED) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "上传已完成");
        }
    }

    private void ensureNotAborted(SessionControl control) {
        if (control.status().get() == ChunkUploadStatus.ABORTED) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "上传已取消");
        }
    }

    private void validateMergedFile(ChunkMeta meta, Path mergedFile) {
        try {
            long mergedSize = Files.size(mergedFile);
            if (mergedSize != meta.totalSize()) {
                throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "合并后的文件大小与初始化声明不一致");
            }
        } catch (IOException ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "校验合并文件大小失败");
        }
    }

    private void cleanupIfNeeded(String uploadId, SessionControl control) {
        if (control.executionLock().isLocked()) {
            return;
        }
        if (control.status().get() == ChunkUploadStatus.ABORTED) {
            Path dir = sessionDir(uploadId);
            if (Files.exists(dir)) {
                deleteRecursively(dir);
            }
            sessionControls.remove(uploadId);
        }
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path sessionDir(String uploadId) {
        return Path.of(storageProperties.getChunk().getTempDir()).resolve(uploadId);
    }

    private Path chunkPath(String uploadId, int chunkIndex) {
        return sessionDir(uploadId).resolve(CHUNKS_DIR).resolve(chunkIndex + ".part");
    }

    private void deleteRecursively(Path path) {
        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(current -> {
                try {
                    Files.deleteIfExists(current);
                } catch (IOException ex) {
                    throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "删除分片临时文件失败");
                }
            });
        } catch (IOException ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "删除分片临时目录失败");
        }
    }

    private record ChunkMeta(String uploadId,
                             String fieldName,
                             String originalFilename,
                             String contentType,
                             long totalSize,
                             int totalChunks,
                             ChunkUploadStatus status) {
        private ChunkMeta withStatus(ChunkUploadStatus newStatus) {
            return new ChunkMeta(uploadId, fieldName, originalFilename, contentType, totalSize, totalChunks, newStatus);
        }
    }

    private record SessionControl(ReentrantLock executionLock, AtomicReference<ChunkUploadStatus> status) {
        private SessionControl(ChunkUploadStatus initialStatus) {
            this(new ReentrantLock(), new AtomicReference<>(initialStatus));
        }
    }

    private static class LocalChunkMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final Path file;

        private LocalChunkMultipartFile(String name, String originalFilename, String contentType, Path file) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.file = file;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return getSize() <= 0;
        }

        @Override
        public long getSize() {
            try {
                return Files.size(file);
            } catch (IOException ex) {
                throw new BizException(ErrorCode.STORAGE_ERROR.getCode(), "读取合并文件大小失败");
            }
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(file);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Files.newInputStream(file);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.copy(file, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
