# polo-boot-api-doc

## 模块作用

`polo-boot-api-doc` 负责接口文档能力，当前主要包括：

- OpenAPI 基础信息自动装配
- Swagger UI 自动装配
- 分组
- 默认错误响应
- 文档中的鉴权说明

适合：

- 后台管理系统
- 对外开放接口
- 需要团队协作时快速查看接口说明

## 关键类

- [`OpenApiAutoConfiguration.java`](../../polo-boot-api-doc/src/main/java/com/polo/boot/api/doc/config/OpenApiAutoConfiguration.java)
- [`OpenApiProperties.java`](../../polo-boot-api-doc/src/main/java/com/polo/boot/api/doc/properties/OpenApiProperties.java)
- [`ApiOperation.java`](../../polo-boot-api-doc/src/main/java/com/polo/boot/api/doc/annotation/ApiOperation.java)

## 接入方式

```xml
<dependency>
    <groupId>io.github.smiling11123</groupId>
    <artifactId>polo-boot-api-doc</artifactId>
    <version>0.1.2</version>
</dependency>
```

## 配置项

```yaml
polo:
  api-doc:
    enabled: true
    title: Demo API
    description: Demo project api docs
    version: v1.0.0
    show-all-apis-group: true
    all-apis-display-name: 全部接口
    persist-authorization: true
    create-default-group: false
    packages-to-scan:
      - com.example.demo.controller
    security-excluded-paths:
      - /auth/login
      - /auth/refresh
    groups:
      - name: auth
        display-name: Authentication
        path:
          - /auth/**
```

## 使用说明

### 1. 配置文档基础信息

通过 `title`、`description`、`version` 设置文档元信息。

### 2. 配置分组

可以通过 `groups` 自定义分组，比如：

- `auth`
- `user`
- `file`

同时保留“全部接口”分组。

### 3. 配置授权持久化

```yaml
polo:
  api-doc:
    persist-authorization: true
```

开启后：

- 在 Swagger UI 右上角 `Authorize` 输入一次 Bearer Token
- 切换分组后仍会保留
- 刷新页面后也会继续保留

如果你希望每次重新输入，可以显式设成 `false`。

### 4. 标注文档注解

```java
@ApiOperation(value = "查询订单", description = "分页查询订单列表")
@GetMapping("/orders")
public Result<?> list() {
    return Result.success();
}
```

## Swagger 地址

默认情况下可访问：

- `/swagger-ui/index.html`
- `/v3/api-docs`

如果配置了分组，还会有：

- `/v3/api-docs/{group}`

补充说明：

- `/swagger-ui/index.html` 是页面
- `/v3/api-docs` 和 `/v3/api-docs/{group}` 是 OpenAPI JSON 数据源
