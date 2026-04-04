package com.polo.demo.controller;

import com.polo.boot.api.doc.annotation.ApiOperation;
import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.model.Result;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.security.annotation.RequireRole;
import com.polo.boot.storage.annotation.UploadFile;
import com.polo.boot.storage.context.UploadContext;
import com.polo.boot.storage.model.ChunkUploadState;
import com.polo.boot.storage.model.UploadOptions;
import com.polo.boot.storage.model.StoredFileMetadata;
import com.polo.boot.storage.model.UploadResult;
import com.polo.boot.storage.service.ChunkUploadService;
import com.polo.boot.storage.service.FileUploadService;
import com.polo.boot.storage.model.OperationResult;
import com.polo.boot.web.annotation.OperationLog;
import com.polo.boot.web.annotation.OperationType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class UploadController {

    @Autowired
    private FileUploadService fileUploadService;
    @Autowired
    private ChunkUploadService chunkUploadService;

    /**
     * 此接口不适合测试，文件上传逻辑在参数解析阶段进行，上传结果回填到uploadResult，
     * 参数解析阶段在aop切面之前并且文件在请求中，所以不适合兼容日志记录
     * 参数只有回填的uploadResult，导致swagger渲染出来的也存在问题，无法选择文件上传并且需要填uploadResult
     * 不推荐使用
     * @param uploadResult
     * @return
     */
    @RequireRole("admin")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "自动上传文件", description = "文件在参数解析阶段完成上传，业务层直接拿 UploadResult 做后续处理")
    @OperationLog(module = "文件中心", type = OperationType.CREATE, desc = "自动上传文件", logResult = true)
    public Result<UploadResult> upload(
            @UploadFile(value = "file", pathPrefix = "demo", allowedExtensions = {"jpg", "jpeg", "png", "pdf"})
            UploadResult uploadResult) {
        uploadResult.setBizType("demo");
        uploadResult.setBizId(1L);
        return Result.success(uploadResult);
    }

    @RequireRole("admin")
    @PostMapping(value = "/upload/manual", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "手动上传文件", description = "业务代码手动触发上传，适合上传后立即落库、绑定业务主键")
    @OperationLog(module = "文件中心", type = OperationType.CREATE, desc = "手动上传文件", logResult = true)
    public Result<UploadResult> uploadManual(@RequestParam("file") MultipartFile file) {
        UploadResult uploadResult = fileUploadService.upload(file, UploadOptions.builder()
                .fieldName("file")
                .pathPrefix("manual")
                .build());
        uploadResult.setBizType("manual-demo");
        uploadResult.setBizId(2L);
        return Result.success(uploadResult);
    }

    @RequireRole("admin")
    @PostMapping("/upload/context")
    @ApiOperation(value = "读取上传上下文", description = "演示 service/controller 如何从 UploadContext 继续处理上传结果")
    @OperationLog(module = "文件中心", type = OperationType.CREATE, desc = "读取上传上下文", logResult = true)
    public Result<Map<String, Object>> uploadWithContext(
            @UploadFile(value = "file", pathPrefix = "context-demo")
            UploadResult uploadResult) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("current", uploadResult);
        result.put("allResults", UploadContext.getResults());
        result.put("resultsByField", UploadContext.getResultsByField());
        return Result.success(result);
    }

    @RequireRole("admin")
    @PostMapping("/chunk/init")
    @ApiOperation(value = "初始化分片上传", description = "使用基本类型参数创建一个新的分片上传会话，返回 uploadId 和当前状态")
    @OperationLog(module = "文件中心", type = OperationType.CREATE, desc = "初始化分片上传", logResult = true)
    public Result<ChunkUploadState> initChunkUpload(@RequestParam(value = "fieldName", required = false) String fieldName,
                                                    @RequestParam("originalFilename") String originalFilename,
                                                    @RequestParam(value = "contentType", required = false) String contentType,
                                                    @RequestParam("totalSize") long totalSize,
                                                    @RequestParam("totalChunks") int totalChunks) {
        return Result.success(chunkUploadService.init(fieldName, originalFilename, contentType, totalSize, totalChunks));
    }

    @RequireRole("admin")
    @GetMapping("/chunk/status")
    @ApiOperation(value = "查询分片上传状态", description = "返回当前 uploadId 已上传的分片索引，用于断点续传")
    @OperationLog(module = "文件中心", type = OperationType.QUERY, desc = "查询分片上传状态", logResult = true)
    public Result<ChunkUploadState> chunkUploadStatus(@RequestParam("uploadId") String uploadId) {
        return Result.success(chunkUploadService.getState(uploadId));
    }


    @RequireRole("admin")
    @PostMapping(value = "/chunk/part", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "上传分片", description = "上传单个分片；带 uploadId 时继续既有会话，不带 uploadId 且 chunkIndex=0 时可自动初始化")
    @OperationLog(module = "文件中心", type = OperationType.CREATE, desc = "文件分片上传", logParams = false)
    public Result<ChunkUploadState> chunkUpload(@RequestParam(value = "uploadId", required = false) String uploadId,
                                                @RequestParam("chunkIndex") int chunkIndex,
                                                @RequestParam(value = "fieldName", required = false) String fieldName,
                                                @RequestParam(value = "originalFilename", required = false) String originalFilename,
                                                @RequestParam(value = "contentType", required = false) String contentType,
                                                @RequestParam(value = "totalSize", required = false) Long totalSize,
                                                @RequestParam(value = "totalChunks", required = false) Integer totalChunks,
                                                @RequestParam("file") MultipartFile file) {
        if (StringUtils.hasText(uploadId)) {
            return Result.success(chunkUploadService.uploadChunk(uploadId, chunkIndex, file));
        }
        if (chunkIndex != 0) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "自动初始化模式下 chunkIndex 必须为 0");
        }
        if (!StringUtils.hasText(originalFilename)) {
            originalFilename = file.getOriginalFilename();
        }
        if (!StringUtils.hasText(contentType)) {
            contentType = file.getContentType();
        }
        if (totalSize == null || totalSize <= 0) {
            totalSize = file.getSize();
        }
        if (totalChunks == null || totalChunks <= 0) {
            totalChunks = 1;
        }
        ChunkUploadState state = chunkUploadService.init(fieldName, originalFilename, contentType, totalSize, totalChunks);
        return Result.success(chunkUploadService.uploadChunk(state.getUploadId(), chunkIndex, file));
    }

    @RequireRole("admin")
    @PostMapping("/chunk/complete")
    @ApiOperation(value = "完成分片上传", description = "合并全部分片并上传到最终对象存储")
    @OperationLog(module = "文件中心", type = OperationType.CREATE, desc = "完成分片上传", logResult = true)
    public Result<UploadResult> completeChunkUpload(@RequestParam("uploadId") String uploadId,
                                                    @RequestParam(value = "pathPrefix", required = false) String pathPrefix) {
        UploadResult uploadResult = chunkUploadService.complete(uploadId, UploadOptions.builder()
                .pathPrefix(pathPrefix)
                .build());
        uploadResult.setBizType("chunk-demo");
        uploadResult.setBizId(3L);
        return Result.success(uploadResult);
    }

    @RequireRole("admin")
    @DeleteMapping("/chunk/abort")
    @ApiOperation(value = "取消分片上传", description = "删除指定 uploadId 对应的临时分片")
    @OperationLog(module = "文件中心", type = OperationType.DELETE, desc = "取消分片上传", logResult = true)
    public Result<Boolean> abortChunkUpload(@RequestParam("uploadId") String uploadId) {
        chunkUploadService.abort(uploadId);
        return Result.success(Boolean.TRUE);
    }

    @GetMapping("/download")
    @ApiOperation(value = "下载文件", description = "支持标准 HTTP Range 断点/分片下载")
    public void download(@RequestParam String filepath,
                         @RequestParam(required = false) String storageType,
                         @RequestParam(required = false) String token,
                         @RequestParam(required = false) Long expires,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        fileUploadService.validateDownloadAccess(filepath, storageType, token, expires);
        StoredFileMetadata metadata = fileUploadService.getMetadata(filepath, storageType);
        response.setHeader("Accept-Ranges", "bytes");
        response.setContentType(StringUtils.hasText(metadata.getContentType()) ? metadata.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(metadata.getFilename(), StandardCharsets.UTF_8));

        ByteRange range = parseRange(request.getHeader("Range"), metadata.getSize());
        if (range == null) {
            response.setContentLengthLong(metadata.getSize());
            try (InputStream is = fileUploadService.download(filepath, storageType)) {
                OutputStream os = response.getOutputStream();
                StreamUtils.copy(is, os);
                os.flush();
            } catch (IOException ex) {
                if (isClientAbort(ex)) {
                    return;
                }
                throw ex;
            }
            return;
        }

        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        response.setHeader("Content-Range", "bytes " + range.start() + "-" + range.end() + "/" + metadata.getSize());
        response.setContentLengthLong(range.end() - range.start() + 1);
        try (InputStream is = fileUploadService.download(filepath, storageType, metadata, range.start(), range.end())) {
            OutputStream os = response.getOutputStream();
            StreamUtils.copy(is, os);
            os.flush();
        } catch (IOException ex) {
            if (isClientAbort(ex)) {
                return;
            }
            throw ex;
        }
    }

    @RequireRole("admin")
    @DeleteMapping("/delete")
    @ApiOperation(value = "删除文件", description = "物理删除存储中的文件及其缩略图")
    @OperationLog(module = "文件中心", type = OperationType.DELETE, desc = "删除文件", logResult = true)
    public Result<OperationResult> delete(@RequestParam String filepath, @RequestParam(required = false) String storageType) {
        return Result.success(fileUploadService.delete(filepath, UploadOptions.builder()
                .storageType(storageType)
                .build()));
    }

    @RequireRole("admin")
    @DeleteMapping("/bucket")
    @ApiOperation(value = "删除存储桶", description = "删除当前存储实现绑定的第三方存储桶；默认关闭，需显式开启配置")
    @OperationLog(module = "文件中心", type = OperationType.DELETE, desc = "删除存储桶", logResult = true)
    public Result<OperationResult> deleteBucket(@RequestParam(required = false) String storageType) {
        return Result.success(fileUploadService.deleteBucket(storageType));
    }

    private ByteRange parseRange(String rangeHeader, long totalSize) {
        if (!StringUtils.hasText(rangeHeader)) {
            return null;
        }
        if (!rangeHeader.startsWith("bytes=")) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "仅支持 bytes 范围下载");
        }
        try {
            String[] segments = rangeHeader.substring(6).split(",", 2);
            String[] values = segments[0].trim().split("-", 2);
            if (values.length != 2) {
                throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "Range 请求头格式错误");
            }
            long start;
            long end;
            if (!StringUtils.hasText(values[0])) {
                long suffixLength = Long.parseLong(values[1]);
                if (suffixLength <= 0) {
                    throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "Range 请求头格式错误");
                }
                start = Math.max(totalSize - suffixLength, 0);
                end = totalSize - 1;
            } else {
                start = Long.parseLong(values[0]);
                end = StringUtils.hasText(values[1]) ? Long.parseLong(values[1]) : totalSize - 1;
            }
            if (start < 0 || end < start || start >= totalSize || end >= totalSize) {
                throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "请求的下载范围无效");
            }
            return new ByteRange(start, end);
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "Range 请求头格式错误");
        }
    }

    private boolean isClientAbort(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String simpleName = current.getClass().getSimpleName();
            if ("AsyncRequestNotUsableException".equals(simpleName) || "ClientAbortException".equals(simpleName)) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("broken pipe")
                        || normalized.contains("connection reset")
                        || normalized.contains("connection aborted")
                        || message.contains("你的主机中的软件中止了一个已建立的连接")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private record ByteRange(long start, long end) {
    }
}
