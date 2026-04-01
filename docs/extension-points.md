# 扩展点与自定义接口手册

这份文档专门说明：哪些接口是给业务项目实现的、什么时候需要实现、实现后框架如何接管。

- 大多数默认实现都用了 `@ConditionalOnMissingBean`
- 也就是说，业务项目只要注册同类型 Bean，starter 默认实现就会自动让位

## 1. Spring Bean 扩展点

这类接口通常需要你在业务项目里提供 `@Bean` 或 `@Component`。

### 1.1 security

#### `AuthorizationProvider`

文件：

- [`AuthorizationProvider.java`](../polo-boot-security/src/main/java/com/polo/boot/security/provider/AuthorizationProvider.java)

作用：

- 根据用户 ID 加载角色和权限点
- `@RequireRole`、`@RequirePermission` 会用到它

什么时候要实现：

- 你要让权限点和角色来自数据库、缓存或外部服务时

最小示例：

```java
@Component
public class DbAuthorizationProvider implements AuthorizationProvider {

    @Override
    public PermissionProfile loadByUserId(Long userId) {
        return new PermissionProfile(
                userId,
                "admin",
                Set.of("admin"),
                Set.of("system:user:list", "system:user:update")
        );
    }
}
```

#### `CurrentPrincipalProvider`

文件：

- [`CurrentPrincipalProvider.java`](../polo-boot-core/src/main/java/com/polo/boot/core/context/CurrentPrincipalProvider.java)

作用：

- 给其他模块提供“当前主体是谁”的统一入口

什么时候要实现：

- 你不用 `polo-boot-security`
- 或者你有自己的认证体系，但还想复用 `mybatis-plus`、`web` 这些模块

最小示例：

```java
@Bean
public CurrentPrincipalProvider currentPrincipalProvider() {
    return () -> new CurrentPrincipal(1L, "admin", "admin");
}
```

#### `SecurityContextFacade`

文件：

- [`SecurityContextFacade.java`](../polo-boot-core/src/main/java/com/polo/boot/core/context/SecurityContextFacade.java)

作用：

- 给 `mybatis-plus` 读取租户、部门、数据权限、审计用户等上下文

什么时候要实现：

- 你不用默认安全模块
- 或者你要从自己的上下文容器里提供 `tenantId / deptId / auditUserId`

最小示例：

```java
@Bean
public SecurityContextFacade securityContextFacade() {
    return new SecurityContextFacade() {
        @Override
        public Long getTenantId() {
            return 1001L;
        }

        @Override
        public Long getDeptId() {
            return 10L;
        }

        @Override
        public String getDataScope() {
            return "DEPT_AND_CHILD";
        }

        @Override
        public Long getAuditUserId() {
            return 1L;
        }

        @Override
        public Boolean isAdmin() {
            return true;
        }
    };
}
```

### 1.2 web

#### `OperatorContextProvider`

文件：

- [`OperatorContextProvider.java`](../polo-boot-web/src/main/java/com/polo/boot/web/spi/OperatorContextProvider.java)

作用：

- 给操作日志提供“当前操作者”信息

什么时候要实现：

- 你不用 `polo-boot-security`
- 或者想自定义操作日志里的操作者名称、ID、类型

最小示例：

```java
@Bean
public OperatorContextProvider operatorContextProvider() {
    return () -> new OperatorContext(1L, "system", "SYSTEM");
}
```

#### `LogRecorder`

文件：

- [`LogRecorder.java`](../polo-boot-web/src/main/java/com/polo/boot/web/model/LogRecorder.java)

作用：

- 接管操作日志最终如何记录

什么时候要实现：

- 你想把操作日志落库、发 MQ、写 ES，而不是只走默认日志输出

最小示例：

```java
@Component
public class DbLogRecorder implements LogRecorder {
    @Override
    public void record(OperationLogRecord record) {
        // 保存到数据库
    }
}
```

### 1.3 storage

#### `FileStorage`

文件：

- [`FileStorage.java`](../polo-boot-storage/src/main/java/com/polo/boot/storage/service/FileStorage.java)

作用：

- 扩展新的底层文件存储实现

什么时候要实现：

- 你不想用默认的 `MINIO / OSS / COS / LOCAL`
- 或者你有公司内部文件中心、私有对象存储

最小示例：

```java
@Component
public class CustomFileStorage implements FileStorage {

    @Override
    public String getType() {
        return "custom";
    }

    @Override
    public String upload(InputStream inputStream, long size, String filepath, String contentType) {
        return filepath;
    }

    @Override
    public boolean delete(String filepath) {
        return true;
    }

    @Override
    public InputStream getInputStream(String filepath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StoredFileMetadata getMetadata(String filepath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(String filepath) {
        return false;
    }
}
```

补充说明：

- 实现好以后，再把 `polo.storage.type` 配成你的 `getType()` 返回值

### 1.4 validation

#### `SensitiveWordProvider`

文件：

- [`SensitiveWordProvider.java`](../polo-boot-validation/src/main/java/com/polo/boot/validation/provider/SensitiveWordProvider.java)

作用：

- 从数据库或其他外部源加载敏感词

什么时候要实现：

- 你希望词库来自数据库
- 或者词库来自配置中心、远程服务

最小示例：

```java
@Component
public class DbSensitiveWordProvider implements SensitiveWordProvider {

    @Override
    public List<SensitiveWordDefinition> loadWords() {
        return List.of(
                new SensitiveWordDefinition("兼职刷单", "GAMBLING", 5),
                new SensitiveWordDefinition("未证实传言", "RUMOR", 4)
        );
    }
}
```

#### `ContentValidationRecordStore`

文件：

- [`ContentValidationRecordStore.java`](../polo-boot-validation/src/main/java/com/polo/boot/validation/service/ContentValidationRecordStore.java)

作用：

- 在保留默认日志记录的同时，把命中的内容校验记录落库

什么时候要实现：

- 你想保留默认日志
- 同时又想把审核记录写数据库

最小示例：

```java
@Component
public class DbContentValidationRecordStore implements ContentValidationRecordStore {
    @Override
    public void save(ContentValidationRecord record) {
        // 保存到数据库
    }
}
```

#### `ContentValidationRecorder`

文件：

- [`ContentValidationRecorder.java`](../polo-boot-validation/src/main/java/com/polo/boot/validation/service/ContentValidationRecorder.java)

作用：

- 完全接管内容校验命中后的记录逻辑

什么时候要实现：

- 你不想用默认日志记录器
- 你想自己决定写日志、落库、发消息的顺序

最小示例：

```java
@Component
public class CustomContentValidationRecorder implements ContentValidationRecorder {
    @Override
    public void record(ContentValidationRecord record) {
        // 自定义记录逻辑
    }
}
```

### 1.5 mybatis-plus

#### `DeptHierarchyProvider`

文件：

- [`DeptHierarchyProvider.java`](../polo-boot-mybatis-plus/src/main/java/com/polo/boot/mybatis/plus/service/DeptHierarchyProvider.java)

作用：

- 给 `DEPT_AND_CHILD` 数据权限提供“子部门集合”

什么时候要实现：

- 你启用了 `DEPT_AND_CHILD`
- 并且你的部门树需要从数据库或组织架构服务解析

最小示例：

```java
@Component
public class DbDeptHierarchyProvider implements DeptHierarchyProvider {
    @Override
    public Set<Long> resolveDeptAndChildren(Long deptId) {
        return Set.of(deptId, 11L, 12L);
    }
}
```

### 1.6 cache

#### `RedisService`

文件：

- [`RedisService.java`](../polo-boot-cache/src/main/java/com/polo/boot/cache/service/RedisService.java)

作用：

- 覆盖默认 RedisService 封装

什么时候要实现：

- 你想替换默认序列化策略
- 或者想把缓存能力对接到公司自己的缓存抽象

说明：

- 这类场景不常见，但默认实现同样支持被业务项目覆盖

## 2. 模型 / 实体兼容接口

这类接口通常不是注册 Bean，而是给你自己的实体类或用户模型 `implements`。

### 2.1 security 用户模型接口

#### `UserPrincipal`

文件：

- [`UserPrincipal.java`](../polo-boot-security/src/main/java/com/polo/boot/security/model/UserPrincipal.java)

作用：

- 定义登录用户的最小契约

必须提供：

- `getPrincipalId()`
- `getPrincipalName()`
- `getPrincipalRole()`

补充说明：

- 如果你已经有自己的登录用户对象，最推荐直接实现这个接口
- 其他租户、部门、权限等扩展信息，可以继续通过字段注解或 `getAttributes()` 提供

#### `SecurityAttributeProvider`

文件：

- [`SecurityAttributeProvider.java`](../polo-boot-security/src/main/java/com/polo/boot/security/model/SecurityAttributeProvider.java)

作用：

- 给登录用户对象提供一组扩展安全属性

什么时候要实现：

- 你不想在用户类上堆很多字段
- 或者这些属性来自动态组装

最小示例：

```java
public class DemoPrincipal implements UserPrincipal, SecurityAttributeProvider {
    @Override
    public Map<String, Object> provideSecurityAttributes() {
        return Map.of(
                "tenantId", 1001L,
                "deptId", 10L,
                "auditUserId", 1L,
                "permissions", List.of("system:user:list")
        );
    }
}
```

### 2.2 mybatis-plus 实体兼容接口

#### `Auditable`

文件：

- [`Auditable.java`](../polo-boot-mybatis-plus/src/main/java/com/polo/boot/mybatis/plus/model/Auditable.java)

作用：

- 兼容老写法，按默认字段名自动填充 `createTime / createBy / updateTime / updateBy`

#### `TenantAware`

文件：

- [`TenantAware.java`](../polo-boot-mybatis-plus/src/main/java/com/polo/boot/mybatis/plus/model/TenantAware.java)

作用：

- 兼容老写法，按默认字段名自动填充 `tenantId`

#### `DataScopeAware`

文件：

- [`DataScopeAware.java`](../polo-boot-mybatis-plus/src/main/java/com/polo/boot/mybatis/plus/model/DataScopeAware.java)

作用：

- 兼容老写法，按默认字段名自动填充 `deptId`

补充说明：

- 这三类接口主要是兼容旧风格
- 当前更推荐直接在实体字段上使用 `@AutoFillField`，并同时配合 MyBatis-Plus 的 `@TableField(fill = ...)`

## 3. 选择建议

如果你不确定该实现哪个接口，可以先按这个思路判断：

- 想接管角色 / 权限加载：实现 `AuthorizationProvider`
- 想接管当前操作者：实现 `OperatorContextProvider`
- 想接管操作日志落点：实现 `LogRecorder`
- 想扩展新的对象存储：实现 `FileStorage`
- 想把内容审核词库接到数据库：实现 `SensitiveWordProvider`
- 想把内容审核记录落库：实现 `ContentValidationRecordStore`
- 想完全接管内容审核记录流程：实现 `ContentValidationRecorder`
- 想给 `DEPT_AND_CHILD` 补子部门查询：实现 `DeptHierarchyProvider`
- 不用默认安全模块但还想复用上下文能力：实现 `CurrentPrincipalProvider` 和 `SecurityContextFacade`
