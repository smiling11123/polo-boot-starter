package com.polo.demo.service;

import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.demo.security.DemoLoginPrincipal;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DemoUserProfileService {
    private static final String DEFAULT_PASSWORD = "123456";
    private final Map<String, DemoAccountProfile> profiles = new LinkedHashMap<>();

    public DemoUserProfileService() {
        register(new DemoAccountProfile(
                "admin",
                DEFAULT_PASSWORD,
                1L,
                "admin",
                1001L,
                10L,
                "ALL",
                true,
                List.of("*:*:*"),
                "超管，拥有全部权限，适合测试全部接口"
        ));
        register(new DemoAccountProfile(
                "manager",
                DEFAULT_PASSWORD,
                2L,
                "manager",
                1001L,
                20L,
                "DEPT_AND_CHILD",
                false,
                List.of(
                        "profile:read:view",
                        "system:user:list",
                        "demo:record:list",
                        "demo:record:page",
                        "demo:record:detail",
                        "demo:record:create",
                        "demo:record:update",
                        "demo:record:scope:dept",
                        "demo:record:scope:custom"
                ),
                "部门经理，可查看本部门及子部门数据，并测试创建、更新、分页和部门数据权限"
        ));
        register(new DemoAccountProfile(
                "auditor",
                DEFAULT_PASSWORD,
                3L,
                "auditor",
                1001L,
                30L,
                "DEPT_ONLY",
                false,
                List.of(
                        "profile:read:view",
                        "demo:record:list",
                        "demo:record:detail",
                        "demo:record:audit:view",
                        "demo:record:scope:dept"
                ),
                "审计员，适合测试审计字段查看和部门范围只读访问"
        ));
        register(new DemoAccountProfile(
                "user",
                DEFAULT_PASSWORD,
                4L,
                "user",
                1001L,
                21L,
                "SELF_ONLY",
                false,
                List.of(
                        "profile:read:view",
                        "demo:record:list",
                        "demo:record:page",
                        "demo:record:detail",
                        "demo:record:create",
                        "demo:record:scope:self",
                        "demo:record:update:self"
                ),
                "普通用户，只能查看和更新自己创建的数据，适合测试 SELF_ONLY 与细粒度权限点"
        ));
    }

    public DemoLoginPrincipal authenticate(String username, String password) {
        DemoAccountProfile profile = profiles.get(username);
        if (profile == null || !profile.password().equals(password)) {
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }
        return profile.toPrincipal();
    }

    public List<Map<String, Object>> listAccounts() {
        return profiles.values().stream()
                .map(profile -> Map.<String, Object>of(
                        "username", profile.username(),
                        "password", profile.password(),
                        "role", profile.role(),
                        "tenantId", profile.tenantId(),
                        "deptId", profile.deptId(),
                        "dataScope", profile.dataScope(),
                        "permissions", profile.permissions(),
                        "description", profile.description()
                ))
                .toList();
    }

    private void register(DemoAccountProfile profile) {
        profiles.put(profile.username(), profile);
    }

    private DemoLoginPrincipal copyPrincipal(DemoLoginPrincipal source) {
        DemoLoginPrincipal target = new DemoLoginPrincipal();
        target.setUserId(source.getUserId());
        target.setUsername(source.getUsername());
        target.setRole(source.getRole());
        target.setTenantId(source.getTenantId());
        target.setDeptId(source.getDeptId());
        target.setDataScope(source.getDataScope());
        target.setAuditUserId(source.getAuditUserId());
        target.setAdminFlag(source.getAdminFlag());
        target.setPermissions(source.getPermissions());
        target.setAttributes(source.getAttributes() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source.getAttributes()));
        return target;
    }

    private record DemoAccountProfile(String username,
                                      String password,
                                      Long userId,
                                      String role,
                                      Long tenantId,
                                      Long deptId,
                                      String dataScope,
                                      boolean admin,
                                      List<String> permissions,
                                      String description) {
        private DemoLoginPrincipal toPrincipal() {
            DemoLoginPrincipal principal = new DemoLoginPrincipal();
            principal.setUserId(userId);
            principal.setUsername(username);
            principal.setRole(role);
            principal.setTenantId(tenantId);
            principal.setDeptId(deptId);
            principal.setDataScope(dataScope);
            principal.setAuditUserId(userId);
            principal.setAdminFlag(admin);
            principal.setPermissions(permissions);
            return principal;
        }
    }
}
