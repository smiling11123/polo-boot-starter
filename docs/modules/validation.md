# polo-boot-validation

## 模块作用

`polo-boot-validation` 提供两类能力：

- 常见自定义校验注解
- 文本内容安全校验

当前内容安全能力支持：

- 敏感词检测
- 正则泄露检测
- 可选 AI 语义检测
- 默认日志记录
- 可选记录落库

## 关键类

- [`ValidationAutoConfiguration.java`](../../polo-boot-validation/src/main/java/com/polo/boot/validation/config/ValidationAutoConfiguration.java)
- [`ValidationProperties.java`](../../polo-boot-validation/src/main/java/com/polo/boot/validation/properties/ValidationProperties.java)
- [`InputContent.java`](../../polo-boot-validation/src/main/java/com/polo/boot/validation/annotation/InputContent.java)
- [`InputContentValidationService.java`](../../polo-boot-validation/src/main/java/com/polo/boot/validation/service/InputContentValidationService.java)
- [`SensitiveWordManager.java`](../../polo-boot-validation/src/main/java/com/polo/boot/validation/service/SensitiveWordManager.java)
- [`AiContentChecker.java`](../../polo-boot-validation/src/main/java/com/polo/boot/validation/service/AiContentChecker.java)

## 接入方式

```xml
<dependency>
    <groupId>com.polo</groupId>
    <artifactId>polo-boot-validation</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## 模块开关

```yaml
polo:
  validation:
    enabled: true
    input-content:
      enabled: true
      default-ai-threshold: 0.8
      ai:
        enabled: false
        api-url: https://example.com/check
        api-key: your-key
      word-library:
        built-in-enabled: true
        built-in-path: sensitive-words/
        dynamic-enabled: false
        redis-key: content:security:words
        refresh-interval: 600000
        database-enabled: false
```

## 常规校验注解

例如手机号校验：

```java
public class UserCreateRequest {
    @Phone
    private String mobile;
}
```

配合 `@Valid` 或 `@Validated` 使用。

## 内容安全校验

### 1. 注解入口

- [`InputContent.java`](../../polo-boot-validation/src/main/java/com/polo/boot/validation/annotation/InputContent.java)

### 2. 使用示例

```java
public class PostCreateRequest {

    @InputContent(
            types = {
                    InputContent.CheckType.SENSITIVE_WORD,
                    InputContent.CheckType.REGEX_PATTERN
            },
            strategy = InputContent.Strategy.REJECT
    )
    private String content;
}
```

### 3. controller 示例

```java
@PostMapping("/posts")
public Result<?> create(@Valid @RequestBody PostCreateRequest request) {
    return Result.success();
}
```

## 词库来源

支持 5 种组合方式：

- 只用内置词库
- 只用 Redis 动态词库
- 只用数据库词库 SPI
- 内置词库 + Redis 动态词库
- 内置词库 + Redis 动态词库 + 数据库词库 SPI

当前统一加载顺序是：

1. 内置词库
2. Redis 动态词库
3. 数据库词库 SPI

无论是启动、定时刷新还是手动刷新，都会按这个顺序重建内存 DFA 词树。

### 内置词库

位置：

- [`polo-boot-validation/src/main/resources/sensitive-words`](../../polo-boot-validation/src/main/resources/sensitive-words)

适合：

- 本地开发
- 默认开箱即用
- 不需要在线动态维护词库

补充说明：

- 文件名去掉扩展名后，就是该词库文件的分类编码
- 目前默认只扫描当前目录，不递归子目录

### Redis 动态词库

初始化脚本：

- [`docs/redis/content-security-words.redis`](../redis/content-security-words.redis)

详细说明：

- [`validation-content-security.md`](../validation-content-security.md)

适合：

- 后台动态加词
- 线上热更新词库
- 不重启服务调整策略

demo 已提供动态词库管理接口：

- `GET /validate/dynamic-words`
- `POST /validate/dynamic-words`
- `DELETE /validate/dynamic-words`
- `POST /validate/dynamic-words/refresh`

它们会直接读写 Redis 动态词库，并立即影响当前应用内存词树，不必等待定时刷新。

本地词库加载也不再依赖固定分类枚举，而是扫描 `built-in-path` 目录下的所有 `.txt` 文件，并把“文件名去掉扩展名”作为分类编码。

### 数据库词库 SPI

如果你希望词库来自数据库，可以在业务项目里实现：

- [`SensitiveWordProvider.java`](../../polo-boot-validation/src/main/java/com/polo/boot/validation/provider/SensitiveWordProvider.java)

接口返回：

- [`SensitiveWordDefinition.java`](../../polo-boot-validation/src/main/java/com/polo/boot/validation/model/SensitiveWordDefinition.java)

开启配置后，starter 会在启动、定时刷新、手动刷新时统一加载这些词条：

```yaml
polo:
  validation:
    input-content:
      word-library:
        database-enabled: true
```

## 默认记录行为

命中内容安全规则后，默认会：

- 打日志
- 如果存在 `ContentValidationRecordStore` Bean，再顺手落库

## 如何覆盖默认行为

### 1. 完全覆盖记录器

自己提供 `ContentValidationRecorder` Bean。

### 2. 保留默认日志并增加持久化

只实现 `ContentValidationRecordStore` 即可。

这两种方式的详细说明，也已经整理在：

- [`validation-content-security.md`](../validation-content-security.md)
