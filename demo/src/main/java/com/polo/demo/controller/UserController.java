package com.polo.demo.controller;

import com.polo.boot.api.doc.annotation.ApiOperation;
import com.polo.boot.core.model.Result;
import com.polo.boot.mybatis.plus.annotation.DataScope;
import com.polo.boot.mybatis.plus.annotation.DataScopeType;
import com.polo.boot.security.annotation.CurrentUser;
import com.polo.boot.security.annotation.CurrentUserAttribute;
import com.polo.boot.security.annotation.RequirePermission;
import com.polo.boot.security.context.SecurityAttributeKeys;
import com.polo.boot.security.model.LoginUser;
import com.polo.boot.web.annotation.OperationLog;
import com.polo.boot.web.annotation.OperationType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("/me")
    @DataScope(type = DataScopeType.SELF_ONLY, userColumn = "user_id")
    @ApiOperation(value = "获取当前用户", description = "返回当前登录用户信息")
    @OperationLog(module = "用户中心", type = OperationType.QUERY, desc = "获取当前用户")
    public Result<LoginUser> me(@CurrentUser LoginUser loginUser) {
        return Result.success(loginUser);
    }

    @GetMapping("/context")
    @ApiOperation(value = "获取用户上下文", description = "查看当前登录上下文和扩展属性")
    @OperationLog(module = "用户中心", type = OperationType.QUERY, desc = "获取用户上下文")
    public Result<Map<String, Object>> context(@CurrentUser LoginUser loginUser) {
        return Result.success(Map.of(
                "userId", loginUser.getUserId(),
                "tenantId", 1,
                "deptId", 1,
                "dataScope", 1,
                "isAdmin", Boolean.TRUE.equals(true),
                "permissions", 1,
                "attributes", loginUser.getAttributes()
        ));
    }

    @RequirePermission("system:user:list")
    @GetMapping("/list")
    @DataScope(type = DataScopeType.DEPT_AND_CHILD, deptColumn = "dept_id")
    @ApiOperation(value = "查询用户列表", description = "管理员查询用户列表示例")
    @OperationLog(module = "用户中心", type = OperationType.QUERY, desc = "查询用户列表")
    public Result<List<Map<String, Object>>> list() {
        return Result.success(List.of(
                Map.of("id", 1, "username", "admin", "role", "admin"),
                Map.of("id", 2, "username", "user", "role", "user")
        ));
    }

    @RequirePermission(value = {"profile:read:view", "system:user:list"}, logical = com.polo.boot.security.annotation.Logical.ANY)
    @GetMapping("/permission-demo")
    @ApiOperation(value = "权限校验示例", description = "演示按权限访问接口")
    @OperationLog(module = "用户中心", type = OperationType.QUERY, desc = "权限校验示例")
    public Result<Map<String, Object>> permissionDemo(@CurrentUser LoginUser loginUser) {
        return Result.success(Map.of(
                "username", loginUser.getUsername(),
                "permissions", loginUser.getPrincipalPermissions()
        ));
    }

    @RequirePermission("*:*:*")
    @GetMapping("/all")
    @DataScope(type = DataScopeType.ALL)
    @ApiOperation(value = "全部数据权限", description = "演示 DataScope ALL（超管可见所有数据）")
    @OperationLog(module = "用户中心", type = OperationType.QUERY, desc = "全部数据权限查询")
    public Result<List<Map<String, Object>>> allScope() {
        return Result.success(List.of(
                Map.of("id", 1, "username", "admin", "deptId", 10, "scope", "ALL"),
                Map.of("id", 2, "username", "user", "deptId", 20, "scope", "ALL"),
                Map.of("id", 3, "username", "guest", "deptId", 30, "scope", "ALL")
        ));
    }

    @GetMapping("/dept-only")
    @DataScope(type = DataScopeType.DEPT_ONLY, deptColumn = "dept_id")
    @ApiOperation(value = "本部门数据权限", description = "演示 DataScope DEPT_ONLY（仅本部门数据）")
    @OperationLog(module = "用户中心", type = OperationType.QUERY, desc = "本部门数据权限查询")
    public Result<List<Map<String, Object>>> deptOnlyScope() {
        return Result.success(List.of(
                Map.of("id", 1, "username", "同部门用户A", "scope", "DEPT_ONLY"),
                Map.of("id", 2, "username", "同部门用户B", "scope", "DEPT_ONLY")
        ));
    }

    @GetMapping("/custom-scope")
    @DataScope(type = DataScopeType.CUSTOM, customCondition = "is_deleted = '1'")
    @ApiOperation(value = "自定义数据权限", description = "演示 DataScope CUSTOM（自定义 SQL 条件）")
    @OperationLog(module = "用户中心", type = OperationType.QUERY, desc = "自定义数据权限查询")
    public Result<List<Map<String, Object>>> customScope() {
        return Result.success(List.of(
                Map.of("id", 1, "username", "自定义条件匹配的用户", "scope", "CUSTOM")
        ));
    }
}
