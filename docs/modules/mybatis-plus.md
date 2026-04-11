# polo-boot-mybatis-plus

## 模块作用

`polo-boot-mybatis-plus` 负责 MyBatis-Plus 相关增强，当前主要包括：

- MyBatis-Plus 自动装配
- 分页
- 乐观锁
- 租户隔离
- 数据权限
- 审计自动填充
- `tenantId` / `deptId` / `createBy` / `updateBy` 等自动填充
- service 层数据权限注解 + Mapper 兜底

## 关键类

- [`MybatisPlusAutoConfiguration.java`](../../polo-boot-mybatis-plus/src/main/java/com/polo/boot/mybatis/plus/config/MybatisPlusAutoConfiguration.java)
- [`MybatisPlusProperties.java`](../../polo-boot-mybatis-plus/src/main/java/com/polo/boot/mybatis/plus/config/MybatisPlusProperties.java)
- [`DataScope.java`](../../polo-boot-mybatis-plus/src/main/java/com/polo/boot/mybatis/plus/annotation/DataScope.java)
- [`DataScopeAspect.java`](../../polo-boot-mybatis-plus/src/main/java/com/polo/boot/mybatis/plus/service/DataScopeAspect.java)
- [`DataScopeInnerInterceptor.java`](../../polo-boot-mybatis-plus/src/main/java/com/polo/boot/mybatis/plus/service/DataScopeInnerInterceptor.java)
- [`SmartMetaObjectHandler.java`](../../polo-boot-mybatis-plus/src/main/java/com/polo/boot/mybatis/plus/service/SmartMetaObjectHandler.java)
- [`AutoFillField.java`](../../polo-boot-mybatis-plus/src/main/java/com/polo/boot/mybatis/plus/annotation/AutoFillField.java)

## 接入方式

```xml
<dependency>
    <groupId>io.github.smiling11123</groupId>
    <artifactId>polo-boot-mybatis-plus</artifactId>
    <version>0.1.1</version>
</dependency>
```

## 基础开关

```yaml
polo:
  mybatis-plus:
    enabled: true
    meta-object-handler-enabled: true
    pagination-enabled: true
    optimistic-locker-enabled: true
```

说明：

- `enabled`：是否启用模块自动装配
- `meta-object-handler-enabled`：是否启用审计与自动填充处理器
- `pagination-enabled`：是否启用分页拦截器
- `optimistic-locker-enabled`：是否启用乐观锁拦截器

## 租户隔离

```yaml
polo:
  mybatis-plus:
    tenant:
      enabled: true
      tenant-id-column: tenant_id
      ignore-if-missing: true
      ignore-tables:
        - sys_config
        - sys_dict
```

说明：

- `tenant.enabled`：是否启用租户拦截
- `tenant-id-column`：租户列名
- `ignore-if-missing`：缺租户上下文时是否忽略
- `ignore-tables`：不受租户影响的表

## 数据权限

```yaml
polo:
  mybatis-plus:
    data-scope:
      enabled: true
      admin-bypass: true
      deny-when-attribute-missing: true
```

### 推荐写法

当前推荐把 `@DataScope` 标在 **Service 方法** 上：

```java
@DataScope(type = DataScopeType.DEPT_AND_CHILD, deptColumn = "o.dept_id")
public List<OrderEntity> pageOrders() {
    return orderMapper.selectList(new LambdaQueryWrapper<>());
}
```

说明：

- service 层注解优先
- Mapper 方法上的 `@DataScope` 仍可作为兜底或精确覆盖
- 更适合 BaseMapper 通用方法场景
- service 切面会先把数据权限规则写进 `DataScopeContext`
- MyBatis 查询执行时，`DataScopeInnerInterceptor` 再读取当前规则并改写 SQL

### 支持范围

- `ALL`
- `DEPT_ONLY`
- `DEPT_AND_CHILD`
- `SELF_ONLY`
- `CUSTOM`

### 子部门支持

如果你要支持“本部门及子部门”，需要自己提供：

- [`DeptHierarchyProvider.java`](../../polo-boot-mybatis-plus/src/main/java/com/polo/boot/mybatis/plus/service/DeptHierarchyProvider.java)

### 异步注意事项

数据权限当前依赖请求线程上下文：

- [`DataScopeContext.java`](../../polo-boot-mybatis-plus/src/main/java/com/polo/boot/mybatis/plus/service/DataScopeContext.java)

所以：

- 普通同步请求链路里可以直接使用
- `@Async`、线程池、MQ 消费、定时任务里不要默认依赖当前数据权限上下文
- 这类场景下更推荐显式传入 `tenantId / deptId / auditUserId`

## 自动填充

推荐在实体字段上使用：

- [`AutoFillField.java`](../../polo-boot-mybatis-plus/src/main/java/com/polo/boot/mybatis/plus/annotation/AutoFillField.java)

同时配合 MyBatis-Plus 的 `@TableField(fill = ...)`，这样 MyBatis-Plus 才会稳定触发 `MetaObjectHandler`。

示例：

```java
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;

public class OrderEntity {

    @TableField(fill = FieldFill.INSERT)
    @AutoFillField(type = AutoFillType.CREATE_TIME)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @AutoFillField(type = AutoFillType.UPDATE_TIME)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    @AutoFillField(type = AutoFillType.CREATE_BY)
    private Long creatorId;

    @TableField(fill = FieldFill.INSERT)
    @AutoFillField(type = AutoFillType.TENANT_ID)
    private Long companyId;

    @TableField(fill = FieldFill.INSERT)
    @AutoFillField(type = AutoFillType.DEPT_ID)
    private Long departmentId;
}
```

更新人字段通常建议写成：

```java
@TableField(fill = FieldFill.INSERT_UPDATE)
@AutoFillField(type = AutoFillType.UPDATE_BY)
private Long updaterId;
```

支持类型：

- `CREATE_TIME`
- `UPDATE_TIME`
- `CREATE_BY`
- `UPDATE_BY`
- `TENANT_ID`
- `DEPT_ID`

如果不想打注解，也兼容旧接口方式：

- `Auditable`
- `TenantAware`
- `DataScopeAware`

但旧方式需要使用默认字段名。

### 重要说明

- 自动填充作用在 **最终传给 MyBatis-Plus 的实体对象** 上
- controller 的 request DTO 不需要也不应该写 `@TableField(fill = ...)`
- 如果你是 `DTO -> Entity -> mapper.updateById(entity)` 这条链，自动填充看的是 `Entity`
- 如果字段没有出现在实体上，或者实体没有交给 MyBatis-Plus 执行更新，自动填充就不会生效

## 乐观锁

开启条件：

- `polo.mybatis-plus.optimistic-locker-enabled=true`
- 实体字段上使用 MyBatis-Plus 的 `@Version`

示例：

```java
public class OrderEntity {
    private Long id;

    @Version
    private Integer version;
}
```

适合场景：

- 后台表单编辑
- 订单状态更新
- 审批流和工单处理
- 多人可能同时修改同一条记录，但冲突不是极高频的业务

## 使用前提

这个模块通常需要和 [`security`](./security.md) 一起使用，因为：

- 租户
- 数据权限
- 审计用户
- 部门

这些上下文通常都来自当前登录用户。

## demo 中怎么直接验证

当前 demo 已经内置了一套 H2 演示数据和接口，不需要额外准备 MySQL。

推荐直接测试：

- `GET /mybatis-demo/accounts`
- `GET /mybatis-demo/overview`
- `GET /mybatis-demo/tenant/list`
- `GET /mybatis-demo/tenant/page`
- `GET /mybatis-demo/scope/dept`
- `GET /mybatis-demo/scope/self`
- `GET /mybatis-demo/scope/custom-approved`
- `POST /mybatis-demo/records`
- `PUT /mybatis-demo/records/{id}`
- `PUT /mybatis-demo/records/{id}/self`
- `GET /mybatis-demo/records/{id}/audit`
- `POST /mybatis-demo/records/{id}/optimistic-lock-conflict`

额外建议看：

- `GET /mybatis-demo/records/{id}/audit`

这个接口会同时返回可见记录和原始表数据，方便核对：

- `createBy / updateBy`
- `createTime / updateTime`
- `tenantId / deptId`

测试账号建议：

- `admin / 123456`：全量验证
- `manager / 123456`：部门及子部门数据权限、创建、更新、分页
- `auditor / 123456`：审计字段查看
- `user / 123456`：`SELF_ONLY` 和“只能更新自己数据”
