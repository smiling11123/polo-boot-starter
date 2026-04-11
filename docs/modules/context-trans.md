# polo-boot-context-trans

## 模块作用

`polo-boot-context-trans` 负责把请求线程里的上下文安全地透传到异步线程，当前默认支持：

- `UserContext`
- `DataScopeContext`
- `UploadContext`
- `MDC`
- `LocaleContext`

它适合这些场景：

- `@Async` 方法
- `CompletableFuture.supplyAsync(...)`
- 默认业务线程池中的异步日志、异步审计、异步查询
- 需要在异步线程里继续读取当前用户、数据权限或上传结果

## 关键类

- [`AsyncConfig.java`](../../polo-boot-context-trans/src/main/java/com/polo/boot/context/trans/config/AsyncConfig.java)
- [`TaskDecoratorAutoConfiguration.java`](../../polo-boot-context-trans/src/main/java/com/polo/boot/context/trans/config/TaskDecoratorAutoConfiguration.java)
- [`ComprehensiveTaskDecorator.java`](../../polo-boot-context-trans/src/main/java/com/polo/boot/context/trans/decorator/ComprehensiveTaskDecorator.java)
- [`ContextTransProperties.java`](../../polo-boot-context-trans/src/main/java/com/polo/boot/context/trans/properties/ContextTransProperties.java)
- [`ContextTransDemoController.java`](../../demo/src/main/java/com/polo/demo/controller/ContextTransDemoController.java)

## 接入方式

```xml
<dependency>
    <groupId>io.github.smiling11123</groupId>
    <artifactId>polo-boot-context-trans</artifactId>
    <version>0.1.1</version>
</dependency>
```

说明：

- 当前模块会依赖 `security`、`mybatis-plus`、`storage` 中的上下文类型
- 如果这些模块本身没有启用，对应上下文透传会自动退化为“跳过”

## 配置项

```yaml
polo:
  context-trans:
    enabled: true
    enable-async: true
    register-default-executors: true
    propagate-mdc: true
    propagate-user-context: true
    propagate-data-scope: true
    propagate-locale: true
    propagate-upload-context: true
    executor:
      core-pool-size: 8
      max-pool-size: 16
      queue-capacity: 100
      keep-alive-seconds: 60
      thread-name-prefix: polo-async-
      wait-for-tasks-to-complete-on-shutdown: true
      await-termination-seconds: 60
    io-executor:
      core-pool-size: 8
      max-pool-size: 16
      queue-capacity: 100
      keep-alive-seconds: 60
      thread-name-prefix: polo-io-
      wait-for-tasks-to-complete-on-shutdown: true
      await-termination-seconds: 60
```

配置说明：

- `enabled`：是否启用上下文透传模块
- `enable-async`：是否自动开启 `@EnableAsync`
- `register-default-executors`：是否自动注册默认 `taskExecutor` 和 `ioExecutor`
- `propagate-mdc`：是否透传日志 `MDC`
- `propagate-user-context`：是否透传当前登录用户上下文
- `propagate-data-scope`：是否透传数据权限上下文
- `propagate-locale`：是否透传 `LocaleContext`
- `propagate-upload-context`：是否透传 `UploadContext`

补充说明：

- `UploadContext` 不是只看 `propagate-upload-context` 就会生效，它还会结合 `polo.storage.enabled`
- 也就是说，只有 `context-trans` 开启并且 `storage` 模块启用时，上传上下文才会真正参与透传

## 工作方式

当前模块使用 `TaskDecorator` 做“捕获 -> 应用 -> 执行 -> 恢复”的透传：

1. 主线程提交异步任务时，先捕获一份上下文快照
2. 任务进入异步线程前，把快照重新写入当前线程
3. 任务执行完成后，在 `finally` 里清理当前线程
4. 如果线程池里原本就有旧上下文，再恢复成执行前状态

这样做的目的，是尽量避免线程池复用导致的上下文污染。

## 默认线程池

模块默认会注册两个线程池：

- `taskExecutor`
- `ioExecutor`

如果业务项目已经自己定义了同名 Bean，starter 会自动让位，不会强行覆盖。

这意味着：

- 想直接开箱即用：保持默认即可
- 想完全自定义线程池：自己提供 `taskExecutor` / `ioExecutor`

## 使用示例

```java
@Service
public class OrderAsyncService {

    private final Executor taskExecutor;

    public OrderAsyncService(@Qualifier("taskExecutor") Executor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public Map<String, Object> inspect() {
        return CompletableFuture.supplyAsync(() -> {
            UserPrincipal user = UserContext.get();
            return Map.of("principalId", user == null ? null : user.getPrincipalId());
        }, taskExecutor).join();
    }
}
```

如果你更喜欢 `@Async`，也可以直接使用：

```java
@Async("taskExecutor")
public CompletableFuture<Void> doAsyncWork() {
    UserPrincipal user = UserContext.get();
    return CompletableFuture.completedFuture(null);
}
```

## demo 测试接口

demo 已经提供了 3 个专门测试透传效果的接口：

- `GET /context-trans/async/user`
- `GET /context-trans/async/data-scope`
- `POST /context-trans/async/upload`

建议重点关注返回值里的：

- `sync`
- `async`
- `uploadContextConfigured`
- `uploadContextEffective`

如果 `uploadContextConfigured=true` 但 `uploadContextEffective=false`，通常说明：

- `polo.storage.enabled=false`
- 或当前请求里根本没有上传结果

## 注意事项

- 透传能解决“线程切换后上下文丢失”，但不建议把它当成所有异步场景的万能解法
- 对关键业务值，仍然建议显式传参，例如 `userId`、`tenantId`、`filepath`
- `UploadContext` 更适合同一个请求链上的短异步任务，不适合长时间后台任务
- 多层异步嵌套时，透传链仍然依赖你最终使用的是带 `TaskDecorator` 的线程池
