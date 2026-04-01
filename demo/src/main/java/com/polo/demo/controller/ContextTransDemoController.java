package com.polo.demo.controller;

import com.polo.boot.api.doc.annotation.ApiOperation;
import com.polo.boot.core.model.Result;
import com.polo.boot.security.annotation.RequireRole;
import com.polo.boot.storage.context.UploadContext;
import com.polo.boot.storage.model.UploadOptions;
import com.polo.boot.storage.model.UploadResult;
import com.polo.boot.storage.service.FileUploadService;
import com.polo.boot.web.annotation.OperationLog;
import com.polo.boot.web.annotation.OperationType;
import com.polo.demo.service.ContextTransDemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/context-trans")
public class ContextTransDemoController {

    @Autowired
    private FileUploadService fileUploadService;
    private static final String DEFAULT_FILE_FIELD = "file";

    private final ContextTransDemoService contextTransDemoService;

    public ContextTransDemoController(ContextTransDemoService contextTransDemoService) {
        this.contextTransDemoService = contextTransDemoService;
    }

    @GetMapping("/async/user")
    @RequireRole({"admin", "manager", "auditor", "user"})
    @ApiOperation(value = "验证用户上下文透传", description = "在异步线程中读取 UserContext，确认当前登录用户信息是否成功透传")
    @OperationLog(module = "上下文透传", type = OperationType.QUERY, desc = "验证用户上下文透传", logResult = true)
    public Result<Map<String, Object>> asyncUser() {
        return Result.success(contextTransDemoService.inspectUserContextAsync());
    }

    @GetMapping("/async/data-scope")
    @RequireRole({"admin", "manager", "auditor", "user"})
    @ApiOperation(value = "验证数据权限上下文透传", description = "在带 @DataScope 的同步方法里启动异步任务，确认 DataScopeContext 是否成功进入异步线程")
    @OperationLog(module = "上下文透传", type = OperationType.QUERY, desc = "验证数据权限上下文透传", logResult = true)
    public Result<Map<String, Object>> asyncDataScope() {
        return Result.success(contextTransDemoService.inspectDataScopeAsync());
    }

    @PostMapping(value = "/async/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireRole({"admin", "manager", "auditor", "user"})
    @ApiOperation(value = "验证上传上下文透传", description = "手动上传文件后写入 UploadContext，再在异步线程中读取上传结果，便于在 Swagger 中直接测试")
    @OperationLog(module = "上下文透传", type = OperationType.CREATE, desc = "验证上传上下文透传", logResult = true)
    public Result<Map<String, Object>> asyncUpload(@RequestParam(DEFAULT_FILE_FIELD) MultipartFile file) {

        UploadResult uploadResult = fileUploadService.upload(file, UploadOptions.builder()
                .fieldName(DEFAULT_FILE_FIELD)
                .pathPrefix("context-trans-demo")
                .build());
        return Result.success(contextTransDemoService.inspectUploadContextAsync(DEFAULT_FILE_FIELD, uploadResult));
    }
}
