# 内容安全校验使用说明

本文档说明 `polo-boot-validation` 里的 `@InputContent` 如何工作、如何接入内置词库、Redis 动态词库、数据库词库 SPI，以及如何把校验记录落库。

## 1. 功能概览

当前 `@InputContent` 支持 3 类检测：

- 敏感词检测：基于 DFA 词树匹配内置词库和 Redis 动态词库
- 敏感信息泄露检测：手机号、身份证号、银行卡号、邮箱
- AI 语义检测：对接第三方内容审核接口

命中后支持 4 种处理策略：

- `REJECT`：直接校验失败
- `MASK`：记录后放行
- `REVIEW`：记录并标记待审核
- `LOG`：只记录，不拦截

## 2. 最小接入方式

### 2.1 DTO 示例

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

### 2.2 Controller 示例

```java
@PostMapping("/posts")
public Result<?> create(@Valid @RequestBody PostCreateRequest request) {
    return Result.success();
}
```

## 3. 配置说明

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

说明：

- `built-in-enabled`：是否启用内置词库
- `built-in-path`：classpath 下内置词库目录
- `dynamic-enabled`：是否启用 Redis 动态词库
- `redis-key`：Redis Hash Key
- `refresh-interval`：动态词库刷新间隔，单位毫秒
- `database-enabled`：是否启用数据库词库 SPI

## 4. 内置词库

仓库当前默认提供了 8 份内置词库文件：

- `politics.txt`
- `pornography.txt`
- `violence.txt`
- `discrimination.txt`
- `gambling.txt`
- `drugs.txt`
- `privacy.txt`
- `advertising.txt`

位置：

- `polo-boot-validation/src/main/resources/sensitive-words/`

格式：

```text
敏感词|等级
```

示例：

```text
刷单返利|5
买粉加微|4
```

补充说明：

- 现在内置词库不是按固定分类枚举去找文件，而是扫描 `built-in-path` 目录下所有 `.txt` 文件
- 文件名去掉扩展名后就是分类编码
- 当前默认只扫描这一层目录，不递归子目录
- 也就是说，你后续自己新增 `rumor.txt`、`言语辱骂.txt` 这类文件，也会被当成合法分类加载

## 5. Redis 动态词库

### 5.1 存储结构

当前实现使用 Redis Hash：

- key：`content:security:words`
- field：敏感词
- value：`分类|等级`

分类现在支持用户自定义字符串，不再强制限制为内部枚举。

如果你想沿用内置推荐分类，建议使用这些编码：

- `POLITICS`
- `PORNOGRAPHY`
- `VIOLENCE`
- `DISCRIMINATION`
- `GAMBLING`
- `DRUGS`
- `PRIVACY`
- `ADVERTISING`

### 5.2 初始化脚本

仓库已提供示例脚本：

- [content-security-words.redis](D:/Java_code/polo-boot-starter/docs/redis/content-security-words.redis)

导入示例：

```bash
redis-cli < docs/redis/content-security-words.redis
```

Windows PowerShell：

```powershell
Get-Content .\docs\redis\content-security-words.redis | redis-cli
```

### 5.3 刷新机制

不是每次请求都查 Redis。

实际流程是：

1. 服务启动时加载内置词库
2. 如果开启动态词库，再从 Redis 加载一次
3. 按 `refresh-interval` 周期定时刷新
4. 请求校验时直接查内存 DFA 词树

所以 Redis 的作用是“动态词库源”，不是实时查询存储。

补充说明：

- 新增动态词会先写入 Redis，再立即加入当前应用内存词树
- 删除动态词会先从 Redis 删除，再立即重建内置词库 + 动态词库
- 即使不调用手动刷新接口，定时任务也会继续按 `refresh-interval` 周期重载

### 5.4 demo 管理接口

demo 已提供 4 个动态词库管理接口，便于直接测试：

- `GET /validate/dynamic-words`
- `POST /validate/dynamic-words`
- `DELETE /validate/dynamic-words`
- `POST /validate/dynamic-words/refresh`

其中新增请求体示例：

```json
{
  "word": "兼职刷单",
  "category": "GAMBLING",
  "level": 5
}
```

也可以直接传自定义分类，例如：

```json
{
  "word": "未证实传言",
  "category": "RUMOR",
  "level": 4
}
```

本地词库文件的分类来源也已经改成“文件名本身”：

- `politics.txt` -> 分类 `POLITICS`
- `rumor.txt` -> 分类 `RUMOR`
- `言语辱骂.txt` -> 分类 `言语辱骂`

也就是说，文件名不需要再和任何内置枚举保持一致，只要是 `built-in-path` 目录下的 `.txt` 文件就会被加载，文件名去掉扩展名后就是分类编码。

这些接口默认要求 `admin` 账号登录后再调用。

## 6. 数据库词库 SPI

如果你希望敏感词从数据库加载，可以在业务项目中实现
[SensitiveWordProvider.java](D:/Java_code/polo-boot-starter/polo-boot-validation/src/main/java/com/polo/boot/validation/provider/SensitiveWordProvider.java)。

接口返回值使用标准模型：

- [SensitiveWordDefinition.java](D:/Java_code/polo-boot-starter/polo-boot-validation/src/main/java/com/polo/boot/validation/model/SensitiveWordDefinition.java)

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

开启配置：

```yaml
polo:
  validation:
    input-content:
      word-library:
        database-enabled: true
```

生效时机：

- 应用启动时
- 定时刷新时
- 调用 `POST /validate/dynamic-words/refresh` 手动刷新时

数据库词库 SPI 和内置词库、Redis 动态词库不是互斥关系，可以同时开启，最终都会合并进内存 DFA 词树。

统一加载顺序是：

1. 内置词库
2. Redis 动态词库
3. 数据库词库 SPI

## 7. 默认记录行为

当前命中内容校验时，会统一生成一条 `ContentValidationRecord`。

默认行为：

- 写一条日志到控制台
- 如果你提供了 `ContentValidationRecordStore` Bean，再顺手调用 `save(record)` 落库

相关接口：

- `ContentValidationRecorder`
- `ContentValidationRecordStore`

## 8. 如何落库

推荐方式是：业务项目自己实现 `ContentValidationRecordStore`。

示例：

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

这样 starter 默认 recorder 会自动变成：

- 先写日志
- 再调用你的 store 落库

## 9. 推荐表结构

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

如果你有租户、用户、业务单据号，也可以再扩：

- `tenant_id`
- `operator_id`
- `biz_type`
- `biz_id`

## 10. 注意事项

- `MASK` 当前会记录并放行，但不会自动回写请求对象字段
- `REPLACE` 枚举已存在，当前还没有单独实现
- AI 检测不开启时，会自动跳过
- 动态词库分类值写错会导致刷新时报错
- 开启 `database-enabled=true` 但没有提供 `SensitiveWordProvider` Bean 时，系统会打印 warning，并跳过数据库词库加载

## 11. 推荐实践

- 开发环境先用内置词库
- 生产环境用“内置词库 + Redis 动态词库 + 数据库词库 SPI”
- 日志保留，数据库记录单独做审计与复核
