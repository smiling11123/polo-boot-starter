package com.polo.demo.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.polo.boot.api.doc.annotation.ApiOperation;
import com.polo.boot.cache.service.RedisService;
import com.polo.boot.core.model.Result;
import com.polo.boot.security.annotation.RequireRole;
import com.polo.boot.web.annotation.OperationLog;
import com.polo.boot.web.annotation.OperationType;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/cache")
public class CacheController {

    private final RedisService redisService;

    public CacheController(RedisService redisService) {
        this.redisService = redisService;
    }

    // ─────────────────── 简单类型缓存 ───────────────────

    @RequireRole("admin")
    @PostMapping("/set")
    @ApiOperation(value = "设置缓存", description = "使用 RedisService.set 设置键值对，支持自定义过期时间")
    @OperationLog(module = "缓存中心", type = OperationType.CREATE, desc = "'设置缓存[' + #p0.key + ']'")
    public Result<Map<String, Object>> set(@RequestBody CacheSetRequest request) {
        redisService.set(request.getKey(), request.getValue(), request.getTimeout(), TimeUnit.SECONDS);
        return Result.success(Map.of(
                "key", request.getKey(),
                "timeout", request.getTimeout(),
                "message", "缓存设置成功"
        ));
    }

    @RequireRole("admin")
    @GetMapping("/get")
    @ApiOperation(value = "获取缓存(简单类型)", description = "使用 RedisService.get(key, Class) 获取简单类型缓存")
    @OperationLog(module = "缓存中心", type = OperationType.QUERY, desc = "'获取缓存[' + #key + ']'")
    public Result<Map<String, Object>> get(@RequestParam String key) {
        String value = redisService.get(key, String.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);
        result.put("value", value);
        result.put("exists", value != null);
        return Result.success(result);
    }

    // ─────────────────── 复杂类型缓存 ───────────────────

    @RequireRole("admin")
    @PostMapping("/set-list")
    @ApiOperation(value = "设置列表缓存", description = "缓存一个 List<String> 类型的值")
    @OperationLog(module = "缓存中心", type = OperationType.CREATE, desc = "'设置列表缓存[' + #p0.key + ']'")
    public Result<Map<String, Object>> setList(@RequestBody CacheListRequest request) {
        redisService.set(request.getKey(), request.getValues(), request.getTimeout(), TimeUnit.SECONDS);
        return Result.success(Map.of(
                "key", request.getKey(),
                "size", request.getValues().size(),
                "message", "列表缓存设置成功"
        ));
    }

    @RequireRole("admin")
    @GetMapping("/get-list")
    @ApiOperation(value = "获取列表缓存(复杂类型)", description = "使用 RedisService.get(key, TypeReference) 获取复杂类型缓存")
    @OperationLog(module = "缓存中心", type = OperationType.QUERY, desc = "'获取列表缓存[' + #key + ']'")
    public Result<Map<String, Object>> getList(@RequestParam String key) {
        List<String> values = redisService.get(key, new TypeReference<List<String>>() {});
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);
        result.put("values", values);
        result.put("exists", values != null);
        return Result.success(result);
    }

    // ─────────────────── 删除缓存 ───────────────────

    @RequireRole("admin")
    @DeleteMapping("/delete")
    @ApiOperation(value = "删除缓存", description = "使用 RedisService.delete 删除指定 key")
    @OperationLog(module = "缓存中心", type = OperationType.DELETE, desc = "'删除缓存[' + #key + ']'")
    public Result<Map<String, String>> delete(@RequestParam String key) {
        redisService.delete(key);
        return Result.success(Map.of("key", key, "message", "缓存已删除"));
    }

    // ─────────────────── 递增操作 ───────────────────

    @RequireRole("admin")
    @PostMapping("/increment")
    @ApiOperation(value = "计数器递增", description = "使用 RedisService.increment 原子递增")
    @OperationLog(module = "缓存中心", type = OperationType.UPDATE, desc = "'计数器递增[' + #key + ']'")
    public Result<Map<String, Object>> increment(@RequestParam String key) {
        Long newValue = redisService.increment(key);
        return Result.success(Map.of("key", key, "currentValue", newValue));
    }

    // ─────────────────── 请求 DTO ───────────────────

    @Data
    public static class CacheSetRequest {
        private String key;
        private String value;
        private long timeout = 300;
    }

    @Data
    public static class CacheListRequest {
        private String key;
        private List<String> values;
        private long timeout = 300;
    }
}
