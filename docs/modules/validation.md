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
    <groupId>io.github.smiling11123</groupId>
    <artifactId>polo-boot-validation</artifactId>
    <version>0.1.1</version>
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

### 功能概览

当前 `@InputContent` 支持 3 类检测：

- 敏感词检测：基于 DFA 词树匹配内置词库、Redis 动态词库和数据库词库
- 敏感信息泄露检测：手机号、身份证号、银行卡号、邮箱
- AI 语义检测：对接第三方内容审核接口

命中后支持 4 种处理策略：

- `REJECT`：直接校验失败
- `MASK`：记录后放行
- `REVIEW`：记录并标记待审核
- `LOG`：只记录，不拦截

### 注解入口

- [`InputContent.java`](../../polo-boot-validation/src/main/java/com/polo/boot/validation/annotation/InputContent.java)

### DTO 示例

```java
public class PostCreateRequest {

    @InputContent(
            message = "内容包含违规信息",
            types = {
                    InputContent.CheckType.SENSITIVE_WORD,
                    InputContent.CheckType.REGEX_PATTERN
            },
            strategy = InputContent.Strategy.REJECT
    )
    private String content;
}
```

### Controller 示例

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

仓库当前默认提供了 8 份示例词库文件：

- `politics.txt`
- `pornography.txt`
- `violence.txt`
- `discrimination.txt`
- `gambling.txt`
- `drugs.txt`
- `privacy.txt`
- `advertising.txt`

格式：

```text
敏感词|等级
```

示例：

```text
刷单返利|5
买粉加微|4
```

适合：

- 本地开发
- 默认开箱即用
- 不需要在线动态维护词库

补充说明：

- 文件名去掉扩展名后，就是该词库文件的分类编码
- 当前会扫描 `built-in-path` 目录下的所有 `.txt` 文件
- 目前默认只扫描当前目录，不递归子目录
- 也就是说，你后续自己新增 `rumor.txt`、`言语辱骂.txt` 这类文件，也会被当成合法分类加载

### Redis 动态词库

初始化脚本：

- [`docs/redis/content-security-words.redis`](../redis/content-security-words.redis)

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

存储结构：

- key：`content:security:words`
- field：敏感词
- value：`分类|等级`

补充说明：

- 不是每次请求都查 Redis，而是把 Redis 作为动态词库源，加载到内存 DFA 词树中
- 新增动态词会先写 Redis，再立即加入当前应用内存词树
- 删除动态词会先删 Redis，再立即重建内置词库 + Redis 动态词库 + 数据库词库
- 即使不调用手动刷新接口，定时任务也会继续按 `refresh-interval` 周期重载

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

示例：

```java
@Component
@RequiredArgsConstructor
public class DbSensitiveWordProvider implements SensitiveWordProvider {

    private final SensitiveWordMapper sensitiveWordMapper;

    @Override
    public List<SensitiveWordDefinition> loadWords() {
        return sensitiveWordMapper.selectEnabledWords().stream()
                .map(entity -> new SensitiveWordDefinition(
                        entity.getWord(),
                        entity.getCategory(),
                        entity.getLevel()
                ))
                .toList();
    }
}
```

## 默认记录行为

命中内容安全规则后，默认会：

- 打日志
- 如果存在 `ContentValidationRecordStore` Bean，再顺手落库

当前命中内容校验时，会统一生成一条 `ContentValidationRecord`。

## 如何覆盖默认行为

### 1. 完全覆盖记录器

自己提供 `ContentValidationRecorder` Bean。

### 2. 保留默认日志并增加持久化

只实现 `ContentValidationRecordStore` 即可。

### 3. 推荐落库方式

推荐由业务项目自己实现 `ContentValidationRecordStore`：

```java
@Service
@RequiredArgsConstructor
public class DbContentValidationRecordStore implements ContentValidationRecordStore {

    private final ContentValidationRecordMapper mapper;

    @Override
    public void save(ContentValidationRecord record) {
        ContentValidationRecordEntity entity = new ContentValidationRecordEntity();
        entity.setDetectorType(record.getDetectorType());
        entity.setStrategy(record.getStrategy());
        entity.setMessage(record.getMessage());
        entity.setContentPreview(record.getContentPreview());
        entity.setMatchedDetail(record.getMatchedDetail());
        entity.setHitCount(record.getHitCount());
        entity.setOccurredAt(record.getOccurredAt());
        mapper.insert(entity);
    }
}
```

这样默认记录器会变成：

- 先写日志
- 再调用你的 store 落库

### 4. 推荐表结构

```sql
create table content_validation_record (
    id bigint primary key auto_increment,
    detector_type varchar(64) not null,
    strategy varchar(32) not null,
    message varchar(500) not null,
    content_preview varchar(500),
    matched_detail varchar(2000),
    hit_count int,
    occurred_at datetime not null
);
```

如果你有租户、用户、业务单据号，也可以扩展：

- `tenant_id`
- `operator_id`
- `biz_type`
- `biz_id`

## 注意事项

- `MASK` 当前会记录并放行，但不会自动回写请求对象字段
- `REPLACE` 枚举已存在，当前还没有单独实现
- AI 检测不开启时，会自动跳过
- 动态词库分类值写错会导致刷新时报错
- 开启 `database-enabled=true` 但没有提供 `SensitiveWordProvider` Bean 时，系统会打印 warning，并跳过数据库词库加载

## 推荐实践

- 开发环境先用内置词库
- 生产环境用“内置词库 + Redis 动态词库 + 数据库词库 SPI”
- 日志保留，数据库记录单独做审计与复核
