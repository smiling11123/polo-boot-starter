package com.polo.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.polo.boot.api.doc.annotation.ApiOperation;
import com.polo.boot.core.model.Result;
import com.polo.boot.mybatis.plus.annotation.AutoFillField;
import com.polo.boot.mybatis.plus.annotation.AutoFillType;
import com.polo.boot.security.annotation.RequirePermission;
import com.polo.boot.web.annotation.OperationLog;
import com.polo.boot.web.annotation.OperationType;
import com.polo.demo.entity.DemoRecordEntity;
import com.polo.demo.service.DemoUserProfileService;
import com.polo.demo.service.MybatisDemoService;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mybatis-demo")
public class MybatisDemoController {
    private final MybatisDemoService mybatisDemoService;
    private final DemoUserProfileService demoUserProfileService;

    public MybatisDemoController(MybatisDemoService mybatisDemoService,
                                 DemoUserProfileService demoUserProfileService) {
        this.mybatisDemoService = mybatisDemoService;
        this.demoUserProfileService = demoUserProfileService;
    }

    @GetMapping("/accounts")
    @ApiOperation(value = "查看 demo 账号矩阵", description = "列出各个测试账号的权限、数据范围和建议使用场景")
    public Result<List<Map<String, Object>>> accounts() {
        return Result.success(demoUserProfileService.listAccounts());
    }

    @GetMapping("/overview")
    @RequirePermission("demo:record:list")
    @ApiOperation(value = "查看 MyBatis 演示总览", description = "对比当前上下文、原始数据总量、租户过滤后的可见数据量")
    @OperationLog(module = "MyBatis 演示", type = OperationType.QUERY, desc = "查看 MyBatis 演示总览")
    public Result<Map<String, Object>> overview() {
        return Result.success(mybatisDemoService.overview());
    }

    @GetMapping("/raw/all")
    @RequirePermission("demo:record:raw")
    @ApiOperation(value = "查看原始表数据", description = "绕过 MyBatis 拦截器，直接返回 demo_record 全表数据，便于对比租户与数据权限效果")
    @OperationLog(module = "MyBatis 演示", type = OperationType.QUERY, desc = "查看原始表数据")
    public Result<List<Map<String, Object>>> rawAll() {
        return Result.success(mybatisDemoService.listAllRawRecords());
    }

    @GetMapping("/tenant/list")
    @RequirePermission("demo:record:list")
    @ApiOperation(value = "查看租户过滤后的列表", description = "不附加数据权限，只验证租户隔离是否生效")
    @OperationLog(module = "MyBatis 演示", type = OperationType.QUERY, desc = "查看租户过滤后的列表")
    public Result<List<DemoRecordEntity>> tenantVisibleList() {
        return Result.success(mybatisDemoService.listTenantVisibleRecords());
    }

    @GetMapping("/tenant/page")
    @RequirePermission("demo:record:page")
    @ApiOperation(value = "分页查询租户数据", description = "验证分页拦截器与租户拦截器可以同时生效")
    @OperationLog(module = "MyBatis 演示", type = OperationType.QUERY, desc = "分页查询租户数据")
    public Result<Page<DemoRecordEntity>> tenantVisiblePage(@RequestParam(defaultValue = "1") long current,
                                                            @RequestParam(defaultValue = "2") long size) {
        return Result.success(mybatisDemoService.pageTenantVisibleRecords(current, size));
    }

    @GetMapping("/scope/dept")
    @RequirePermission("demo:record:scope:dept")
    @ApiOperation(value = "查看部门及子部门数据", description = "验证 DataScope DEPT_AND_CHILD 与部门层级提供器是否生效")
    @OperationLog(module = "MyBatis 演示", type = OperationType.QUERY, desc = "查看部门及子部门数据")
    public Result<List<DemoRecordEntity>> deptScope() {
        return Result.success(mybatisDemoService.listDeptScopedRecords());
    }

    @GetMapping("/scope/self")
    @RequirePermission("demo:record:scope:self")
    @ApiOperation(value = "查看本人数据", description = "验证 DataScope SELF_ONLY 只返回当前审计人创建的数据")
    @OperationLog(module = "MyBatis 演示", type = OperationType.QUERY, desc = "查看本人数据")
    public Result<List<DemoRecordEntity>> selfScope() {
        return Result.success(mybatisDemoService.listSelfScopedRecords());
    }

    @GetMapping("/scope/custom-approved")
    @RequirePermission("demo:record:scope:custom")
    @ApiOperation(value = "查看自定义数据权限结果", description = "验证 DataScope CUSTOM 会把自定义 SQL 条件拼接到查询中")
    @OperationLog(module = "MyBatis 演示", type = OperationType.QUERY, desc = "查看自定义数据权限结果")
    public Result<List<DemoRecordEntity>> customScope() {
        return Result.success(mybatisDemoService.listApprovedRecords());
    }

    @GetMapping("/records/{id}")
    @RequirePermission("demo:record:detail")
    @ApiOperation(value = "查看单条可见记录", description = "验证在当前租户下按主键读取的可见数据")
    @OperationLog(module = "MyBatis 演示", type = OperationType.QUERY, desc = "'查看记录[' + #id + ']'")
    public Result<DemoRecordEntity> detail(@PathVariable Long id) {
        return Result.success(mybatisDemoService.getVisibleRecord(id));
    }

    @GetMapping("/records/{id}/audit")
    @RequirePermission("demo:record:audit:view")
    @ApiOperation(value = "查看审计与自动填充结果", description = "返回当前记录的可见数据与原始表数据，方便核对 createBy/updateBy/createTime/updateTime/tenantId/deptId")
    @OperationLog(module = "MyBatis 演示", type = OperationType.QUERY, desc = "'查看审计记录[' + #id + ']'")
    public Result<Map<String, Object>> audit(@PathVariable Long id) {
        return Result.success(mybatisDemoService.getAuditDetail(id));
    }

    @PostMapping("/records")
    @RequirePermission("demo:record:create")
    @ApiOperation(value = "创建演示记录", description = "验证 insert 时的审计、自动填充和租户字段填充")
    @OperationLog(module = "MyBatis 演示", type = OperationType.CREATE, desc = "'创建演示记录[' + #request.title + ']'")
    public Result<Map<String, Object>> create(@RequestBody CreateRecordRequest request) {
        return Result.success(mybatisDemoService.createRecord(request.getTitle(), request.getContent(), request.getStatus()));
    }

    @PutMapping("/records/{id}")
    @RequirePermission("demo:record:update")
    @ApiOperation(value = "更新演示记录", description = "验证 updateBy、updateTime 自动填充和正常的乐观锁更新")
    @OperationLog(module = "MyBatis 演示", type = OperationType.UPDATE, desc = "'更新演示记录[' + #id + ']'")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody UpdateRecordRequest request) {
        return Result.success(mybatisDemoService.updateRecord(id, request.getVersion(), request.getTitle(), request.getContent(), request.getStatus()));
    }

    @PutMapping("/records/{id}/self")
    @RequirePermission("demo:record:update:self")
    @ApiOperation(value = "更新本人记录", description = "结合细粒度权限点与 SELF_ONLY 数据权限，只允许更新自己创建的记录")
    @OperationLog(module = "MyBatis 演示", type = OperationType.UPDATE, desc = "'更新本人记录[' + #id + ']'")
    public Result<Map<String, Object>> updateSelf(@PathVariable Long id, @RequestBody UpdateRecordRequest request) {
        return Result.success(mybatisDemoService.updateOwnRecord(id, request.getVersion(), request.getTitle(), request.getContent(), request.getStatus()));
    }

    @PostMapping("/records/{id}/optimistic-lock-conflict")
    @RequirePermission("demo:record:update")
    @ApiOperation(value = "模拟乐观锁冲突", description = "同一版本先后发起两次更新，第二次应该失败，用于验证乐观锁拦截器")
    @OperationLog(module = "MyBatis 演示", type = OperationType.UPDATE, desc = "'模拟乐观锁冲突[' + #id + ']'")
    public Result<Map<String, Object>> optimisticLockConflict(@PathVariable Long id) {
        return Result.success(mybatisDemoService.simulateOptimisticLock(id));
    }

    @Data
    public static class CreateRecordRequest {
        private String title;
        private String content;
        private String status = "DRAFT";
    }

    @Data
    public static class UpdateRecordRequest {
        private Integer version;
        private String title;
        private String content;
        private String status;
    }
}
