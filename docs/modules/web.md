# polo-boot-web

## 模块作用

`polo-boot-web` 负责请求层的通用能力，当前主要包括：

- 全局异常处理
- 操作日志切面

这个模块适合放在所有 Web API 项目里，即使你暂时不需要 `security`，也可以单独使用。

## 关键类

- [`WebAutoConfiguration.java`](../../polo-boot-web/src/main/java/com/polo/boot/web/config/WebAutoConfiguration.java)
- [`WebProperties.java`](../../polo-boot-web/src/main/java/com/polo/boot/web/properties/WebProperties.java)
- [`GlobalExceptionHandler.java`](../../polo-boot-web/src/main/java/com/polo/boot/web/handler/GlobalExceptionHandler.java)
- [`OperationLog.java`](../../polo-boot-web/src/main/java/com/polo/boot/web/annotation/OperationLog.java)
- [`OperationLogAspect.java`](../../polo-boot-web/src/main/java/com/polo/boot/web/aspect/OperationLogAspect.java)

## 接入方式

```xml
<dependency>
    <groupId>com.polo</groupId>
    <artifactId>polo-boot-web</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## 配置项

```yaml
polo:
  web:
    enabled: true
    exception-handler-enabled: true
    operation-log-enabled: true
```

配置说明：

- `enabled`：是否启用 web 模块自动装配
- `exception-handler-enabled`：是否启用全局异常处理
- `operation-log-enabled`：是否启用操作日志切面

## 使用说明

### 1. 全局异常处理

引入模块并保持默认开关即可使用。  
常见异常会被统一转换成标准响应，包括：

- 业务异常
- 参数校验异常
- 请求参数缺失
- JSON 反序列化错误
- 未知异常

如果你业务里直接抛：

```java
throw new BizException("订单不存在");
```

在接口层会自动返回统一结构的失败结果。

### 2. 操作日志

在 controller 或 service 方法上添加：

```java
@OperationLog(module = "订单中心", desc = "创建订单")
@PostMapping("/orders")
public Result<?> create(@RequestBody OrderCreateRequest request) {
    return Result.success();
}
```

常见参数包括：

- `module`：模块名
- `desc`：操作描述
- `type`：操作类型
- `logResult`：是否记录返回结果

## 日志输出说明

当前默认走 Spring Boot 的标准日志体系，也就是：

- 控制台
- 你自己配置的文件输出
- 你自己接的 ELK / Loki / 其他日志系统

`polo-boot-web` 本身不强制把日志落库。  
如果你需要操作日志持久化，建议在后续扩展专门的日志存储实现。

## 和 `api-doc` 的关系

现在 [`ApiOperation`](../../polo-boot-api-doc/src/main/java/com/polo/boot/api/doc/annotation/ApiOperation.java) 已经和 `OperationLog` 解耦：

- `@ApiOperation` 只负责文档
- `@OperationLog` 只负责日志

如果你两者都需要，请显式同时标注。
