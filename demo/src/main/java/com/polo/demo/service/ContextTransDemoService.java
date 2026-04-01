package com.polo.demo.service;

import com.polo.boot.context.trans.properties.ContextTransProperties;
import com.polo.boot.mybatis.plus.annotation.DataScope;
import com.polo.boot.mybatis.plus.annotation.DataScopeType;
import com.polo.boot.mybatis.plus.service.DataScopeContext;
import com.polo.boot.security.context.UserContext;
import com.polo.boot.security.model.UserPrincipal;
import com.polo.boot.storage.context.UploadContext;
import com.polo.boot.storage.model.UploadResult;
import com.polo.boot.storage.properties.StorageProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class ContextTransDemoService {
    private final Executor taskExecutor;
    private final ContextTransProperties contextTransProperties;
    private final StorageProperties storageProperties;

    public ContextTransDemoService(@Qualifier("taskExecutor") Executor taskExecutor,
                                   ContextTransProperties contextTransProperties,
                                   StorageProperties storageProperties) {
        this.taskExecutor = taskExecutor;
        this.contextTransProperties = contextTransProperties;
        this.storageProperties = storageProperties;
    }

    public Map<String, Object> inspectUserContextAsync() {
        Map<String, Object> result = baseResult();
        result.put("sync", captureContext(null));
        result.put("async", CompletableFuture.supplyAsync(() -> captureContext(null), taskExecutor).join());
        return result;
    }

    @DataScope(type = DataScopeType.DEPT_AND_CHILD, deptColumn = "dept_id")
    public Map<String, Object> inspectDataScopeAsync() {
        Map<String, Object> result = baseResult();
        result.put("sync", captureContext(null));
        result.put("async", CompletableFuture.supplyAsync(() -> captureContext(null), taskExecutor).join());
        return result;
    }

    public Map<String, Object> inspectUploadContextAsync(String fieldName, UploadResult currentUpload) {
        Map<String, Object> result = baseResult();
        result.put("currentUpload", simplifyUpload(currentUpload));
        result.put("sync", captureContext(fieldName));
        result.put("async", CompletableFuture.supplyAsync(() -> captureContext(fieldName), taskExecutor).join());
        return result;
    }

    private Map<String, Object> baseResult() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contextTransEnabled", contextTransProperties.isEnabled());
        result.put("taskExecutorClass", taskExecutor.getClass().getName());
        result.put("storageEnabled", storageProperties.isEnabled());
        result.put("uploadContextConfigured", contextTransProperties.isPropagateUploadContext());
        result.put("uploadContextEffective", contextTransProperties.isPropagateUploadContext() && storageProperties.isEnabled());
        return result;
    }

    private Map<String, Object> captureContext(String fieldName) {
        Map<String, Object> result = new LinkedHashMap<>();
        UserPrincipal userPrincipal = UserContext.get();
        result.put("threadName", Thread.currentThread().getName());
        result.put("userPresent", userPrincipal != null);
        if (userPrincipal != null) {
            result.put("principalId", userPrincipal.getPrincipalId());
            result.put("principalName", userPrincipal.getPrincipalName());
            result.put("principalRole", userPrincipal.getPrincipalRole());
            result.put("tenantId", userPrincipal.getAttribute("tenantId"));
            result.put("deptId", userPrincipal.getAttribute("deptId"));
        }

        DataScopeContext.current().ifPresentOrElse(scope -> {
            result.put("dataScopeType", scope.type().name());
            result.put("deptColumn", scope.deptColumn());
            result.put("userColumn", scope.userColumn());
            result.put("customCondition", scope.customCondition());
        }, () -> result.put("dataScopeType", null));

        if (fieldName != null) {
            result.put("uploadContext", simplifyUpload(UploadContext.getFirst(fieldName).orElse(null)));
            result.put("uploadFields", UploadContext.getResultsByField().keySet());
        }
        return result;
    }

    private Map<String, Object> simplifyUpload(UploadResult uploadResult) {
        if (uploadResult == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("uploadId", uploadResult.getUploadId());
        result.put("fieldName", uploadResult.getFieldName());
        result.put("originalFilename", uploadResult.getOriginalFilename());
        result.put("filename", uploadResult.getFilename());
        result.put("filepath", uploadResult.getFilepath());
        result.put("storageType", uploadResult.getStorageType());
        return result;
    }
}
