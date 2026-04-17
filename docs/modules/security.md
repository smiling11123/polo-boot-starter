# polo-boot-security

## 模块作用

`polo-boot-security` 是当前项目能力最完整的模块之一，主要提供：

- JWT 鉴权
- 无状态单 token 模式
- 会话模式单 token
- 双 token + refresh token
- 多设备登录与设备管理
- `@CurrentUser`
- `@CurrentUserAttribute`
- `@RequireRole`
- `@RequirePermission`
- 防重复提交

## 关键类

- [`SecurityAutoConfiguration.java`](../../polo-boot-security/src/main/java/com/polo/boot/security/config/SecurityAutoConfiguration.java)
- [`SecurityProperties.java`](../../polo-boot-security/src/main/java/com/polo/boot/security/properties/SecurityProperties.java)
- [`JwtProperties.java`](../../polo-boot-security/src/main/java/com/polo/boot/security/properties/JwtProperties.java)
- [`AuthInterceptor.java`](../../polo-boot-security/src/main/java/com/polo/boot/security/interceptor/AuthInterceptor.java)
- [`JwtService.java`](../../polo-boot-security/src/main/java/com/polo/boot/security/service/JwtService.java)
- [`TokenService.java`](../../polo-boot-security/src/main/java/com/polo/boot/security/service/TokenService.java)
- [`UserContext.java`](../../polo-boot-security/src/main/java/com/polo/boot/security/context/UserContext.java)

## 接入方式

```xml
<dependency>
    <groupId>io.github.smiling11123</groupId>
    <artifactId>polo-boot-security</artifactId>
    <version>0.1.2</version>
</dependency>
```

## 模块开关

```yaml
polo:
  security:
    enabled: true
    auth-enabled: true
    current-user-resolver-enabled: true
    duplicate-submit-enabled: true
```

配置说明：

- `enabled`：是否启用 security 模块
- `auth-enabled`：是否启用鉴权主链路
- `current-user-resolver-enabled`：是否启用 `@CurrentUser`、`@CurrentUserAttribute`
- `duplicate-submit-enabled`：是否启用防重复提交切面

## JWT 核心配置

```yaml
polo:
  security:
    jwt:
      session-enabled: true
      secret: your-32-bytes-secret
      access-token-expire: 30
      refresh-token-expire: 10080
      refresh-rotation: true
      max-devices: 3
      allow-concurrent-login: true
      allow-token-pair: true
```

## 推荐使用模式

### 1. 纯 JWT 单 token

适合：

- 简单接口鉴权
- 不需要服务端会话
- 不需要设备管理

```yaml
polo:
  security:
    jwt:
      session-enabled: false
      allow-token-pair: false
      secret: your-32-bytes-secret
      access-token-expire: 30
```

### 2. 单 token + 会话

适合：

- 需要退出登录、踢设备
- 不需要 refresh token

```yaml
polo:
  security:
    jwt:
      session-enabled: true
      allow-token-pair: false
      secret: your-32-bytes-secret
      access-token-expire: 30
```

### 3. 双 token + 多设备

适合：

- 需要 refresh token
- 需要多设备管理
- 需要在线设备列表

```yaml
polo:
  security:
    jwt:
      session-enabled: true
      allow-token-pair: true
      secret: your-32-bytes-secret
      access-token-expire: 30
      refresh-token-expire: 10080
      refresh-rotation: true
      max-devices: 3
      allow-concurrent-login: true
```

## 当前用户注入

### 1. 获取当前用户

```java
@GetMapping("/me")
public Result<?> me(@CurrentUser DemoLoginPrincipal user) {
    return Result.success(user);
}
```

只要 `DemoLoginPrincipal` 自己在字段上标了主身份注解，就不需要再实现 `UserPrincipal` 或继承 `LoginUser`。

### 2. 获取当前上下文属性

```java
@GetMapping("/tenant")
public Result<?> tenant(@CurrentUserAttribute("tenantId") Long tenantId) {
    return Result.success(tenantId);
}
```

### 3. 不使用注解时

```java
UserContext.get();
UserContext.getAttribute("tenantId", Long.class);
```

## 权限控制

### 1. 角色控制

```java
@RequireRole("admin")
```

### 2. 权限点控制

```java
@RequirePermission("system:user:list")
```

```java
@RequirePermission(
        value = {"profile:read:view", "system:user:list"},
        logical = Logical.ANY
)
```

## 用户对象需要提供什么

现在支持两种接入方式：

- 兼容旧写法：实现 `UserPrincipal`
- 更推荐：直接用业务自己的普通对象，在字段上标注：
  - `@SecurityPrincipalField`
  - `@SecurityAttributeField`
  - `@SecurityAttributesField`（如果还想保留一份原始扩展属性 Map）

示例：

```java
public class DemoLoginPrincipal {
    @SecurityPrincipalField(type = SecurityPrincipalType.PRINCIPAL_ID)
    private Long userId;

    @SecurityPrincipalField(type = SecurityPrincipalType.PRINCIPAL_NAME)
    private String username;

    @SecurityPrincipalField(type = SecurityPrincipalType.PRINCIPAL_ROLE)
    private String role;

    @SecurityAttributeField(type = SecurityAttributeType.TENANT_ID)
    private Long tenantId;
}
```

demo 参考：

- [`AuthController.java`](../../demo/src/main/java/com/polo/demo/controller/AuthController.java)

## 设备信息来源

登录时设备信息支持两种来源：

- 业务方显式传入 `ClientDevice`
- starter 从请求头、Cookie、User-Agent、IP 自动解析

## 依赖说明

如果启用会话模式或多设备模式，通常需要 Redis 支持，因为服务端需要保存：

- refresh token 校验信息
- 登录会话
- 在线设备列表
- 强制下线状态
