# 项目自定义注解使用手册

这份文档只介绍 `polo-boot-starter` 里**项目自定义注解**的用途、写法和字段含义。

---

## 1. 阅读说明

本文只覆盖项目自定义注解，按模块分组：

- `security`
- `storage`
- `web`
- `api-doc`
- `mybatis-plus`
- `validation`

说明约定：

- “使用位置”只说明推荐标注位置
- “字段说明”只解释这个项目真正会用到的字段
- `message / groups / payload` 这类标准 Bean Validation 字段，不在每个注解里重复解释

---

## 2. security 模块

### 2.1 `@CurrentUser`

**作用**

把当前登录用户直接注入到 controller 方法参数中。

**使用位置**

- 方法参数

**示例**

```java
@GetMapping("/me")
public Result<?> me(@CurrentUser LoginUser user) {
    return Result.success(user);
}
```

**字段说明**

这个注解没有字段，它只是一个标记：

- 标了它，参数解析器就会从 `UserContext` 里取当前用户

**适用场景**

- 获取当前登录用户
- 避免在 controller 里手写 `UserContext.get()`

---

### 2.2 `@CurrentUserAttribute`

**作用**

把当前用户上下文里的某个扩展属性直接注入到 controller 方法参数中。

**使用位置**

- 方法参数

**示例**

```java
@GetMapping("/tenant")
public Result<?> tenant(@CurrentUserAttribute("tenantId") Long tenantId) {
    return Result.success(tenantId);
}
```

**字段说明**

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `value` | `String` | 无 | 要读取的上下文属性 key，例如 `tenantId`、`deptId` |
| `required` | `boolean` | `true` | 是否必须存在；为 `true` 时拿不到值会报错 |

**适用场景**

- 注入 `tenantId`
- 注入 `deptId`
- 注入 `dataScope`
- 注入 `auditUserId`

---

### 2.3 `@RequireRole`

**作用**

声明接口或类需要具备指定角色才能访问。

**使用位置**

- 类
- 方法

**示例**

```java
@RequireRole("admin")
@GetMapping("/list")
public Result<?> list() {
    return Result.success();
}
```

**字段说明**

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `value` | `String[]` | 无 | 允许访问的角色列表 |

**说明**

- 一般一个角色就够用，例如 `@RequireRole("admin")`
- 标在类上表示整个 controller 默认都要这个角色

---

### 2.4 `@RequirePermission`

**作用**

声明接口或类需要具备指定权限点才能访问。

**使用位置**

- 类
- 方法

**示例**

```java
@RequirePermission("system:user:list")
@GetMapping("/list")
public Result<?> list() {
    return Result.success();
}
```

```java
@RequirePermission(
        value = {"profile:read:view", "system:user:list"},
        logical = Logical.ANY
)
```

**字段说明**

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `value` | `String[]` | 无 | 权限点列表 |
| `logical` | `Logical` | `ALL` | 多个权限之间的关系 |

**`Logical` 取值**

| 取值 | 含义 |
| --- | --- |
| `ALL` | 要求拥有全部权限 |
| `ANY` | 拥有其中一个即可 |

---

### 2.5 `@PreventDuplicateSubmit`

**作用**

对接口做防重复提交保护，常用于创建、支付、提交审批、重复点击按钮等场景。

**使用位置**

- 方法

**示例**

```java
@PreventDuplicateSubmit(interval = 10, message = "请勿重复提交")
@PostMapping("/orders")
public Result<?> create(@RequestBody OrderCreateRequest request) {
    return Result.success();
}
```

**字段说明**

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `interval` | `int` | `5` | 防重时间窗口 |
| `message` | `String` | `"操作过于频繁，请稍后再试"` | 重复提交时返回的提示 |
| `strategy` | `KeyStrategy` | `USER_AND_METHOD` | 幂等 key 生成策略 |
| `keyExpression` | `String` | `""` | 自定义 SpEL 表达式；仅在 `CUSTOM` 策略下生效 |
| `immediateRelease` | `boolean` | `false` | 方法执行完成后是否立即释放锁 |
| `timeUnit` | `TimeUnit` | `SECONDS` | 时间单位 |

**`KeyStrategy` 取值**

| 取值 | 含义 |
| --- | --- |
| `USER_AND_METHOD` | 用户 ID + 方法签名 |
| `USER_AND_URI` | 用户 ID + 请求 URI |
| `METHOD_ONLY` | 仅方法签名，所有用户共享 |
| `CUSTOM` | 使用 `keyExpression` 自定义 key |

**适用场景**

- 创建订单
- 提交表单
- 发起支付
- 审批提交流程

---

### 2.6 `@RateLimit`

**作用**

为接口添加限流能力。

**使用位置**

- 类
- 方法

**示例**

```java
@RateLimit(type = RateLimit.LimitType.USER, rate = 5, capacity = 10)
@PostMapping("/send-code")
public Result<?> sendCode() {
    return Result.success();
}
```

**字段说明**

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `type` | `LimitType` | `DEFAULT` | 限流维度 |
| `key` | `String` | `""` | 限流 key 前缀，支持 SpEL |
| `rate` | `double` | `10` | 速率，通常可理解为每秒允许请求数 |
| `capacity` | `long` | `100` | 容量，令牌桶容量或窗口大小 |
| `algorithm` | `Algorithm` | `TOKEN_BUCKET` | 限流算法 |
| `strategy` | `Strategy` | `REJECT` | 限流后的处理方式 |
| `timeout` | `long` | `0` | 等待超时时间，只有 `WAIT` 有效 |
| `fallback` | `String` | `""` | 降级方法名，只有 `FALLBACK` 有效 |
| `message` | `String` | `"请求过于频繁，请稍后再试"` | 限流提示 |

**`LimitType` 取值**

| 取值 | 含义 |
| --- | --- |
| `DEFAULT` | 接口级默认限流 |
| `USER` | 按用户限流 |
| `IP` | 按 IP 限流 |
| `CUSTOM` | 自定义 key 限流 |

**`Algorithm` 取值**

| 取值 | 含义 |
| --- | --- |
| `TOKEN_BUCKET` | 令牌桶，平滑限流，允许一定突发 |
| `SLIDING_WINDOW` | 滑动窗口，统计更精确 |
| `FIXED_WINDOW` | 固定窗口，实现简单 |

**`Strategy` 取值**

| 取值 | 含义 |
| --- | --- |
| `REJECT` | 直接拒绝 |
| `WAIT` | 等待令牌 |
| `FALLBACK` | 执行降级方法 |

---

### 2.7 `@SecurityAttributeField`

**作用**

标记登录用户对象中的某个字段属于哪种“安全上下文属性”。

**使用位置**

- 字段

**示例**

```java
public class DemoLoginPrincipal extends LoginUser {

    @SecurityAttributeField(type = SecurityAttributeType.TENANT_ID)
    private Long tenantId;

    @SecurityAttributeField(type = SecurityAttributeType.PERMISSIONS)
    private List<String> permissions;
}
```

**字段说明**

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `type` | `SecurityAttributeType` | 无 | 当前字段在安全上下文里的语义 |

**`SecurityAttributeType` 取值**

| 取值 | 含义 |
| --- | --- |
| `TENANT_ID` | 租户 ID |
| `DEPT_ID` | 部门 ID |
| `DATA_SCOPE` | 数据权限范围 |
| `IS_ADMIN` | 是否管理员 |
| `AUDIT_USER_ID` | 审计用户 ID |
| `PERMISSIONS` | 权限点集合 |

**适用场景**

- 登录用户对象扩展字段映射
- 为租户、数据权限、审计自动填充提供上下文

---

## 3. storage 模块

### 3.1 `@UploadFile`

**作用**

在 controller 方法参数解析阶段完成文件上传，并把结果直接注入为 `UploadResult` 或 `List<UploadResult>`。

**使用位置**

- 方法参数

**参数类型要求**

- 单文件：`UploadResult`
- 多文件：`List<UploadResult>`

这个注解不会把原始 `MultipartFile` 注入给你。  
文件本体来自 `multipart/form-data` 请求里的对应字段，参数解析器会先读取并上传，再把结构化结果传给 controller。

**示例**

```java
@PostMapping("/upload")
public Result<UploadResult> upload(
        @UploadFile(value = "file", pathPrefix = "avatars", allowedExtensions = {"jpg", "png"})
        UploadResult uploadResult) {
    return Result.success(uploadResult);
}
```

多文件示例：

```java
@PostMapping("/upload/batch")
public Result<List<UploadResult>> uploadBatch(
        @UploadFile(value = "files", pathPrefix = "attachments")
        List<UploadResult> uploadResults) {
    return Result.success(uploadResults);
}
```

**字段说明**

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `value` | `String` | `""` | Multipart 表单字段名；为空时优先取参数名，最后回退为 `file` |
| `required` | `boolean` | `true` | 是否必须上传文件 |
| `allowedTypes` | `String[]` | `{}` | MIME 类型白名单 |
| `allowedExtensions` | `String[]` | `{}` | 文件扩展名白名单 |
| `maxSize` | `long` | `-1` | 单文件最大字节数；`-1` 表示跟随全局配置 |
| `generateThumbnail` | `Toggle` | `DEFAULT` | 是否生成缩略图；`DEFAULT` 表示跟随全局配置 |
| `thumbnailWidth` | `int` | `-1` | 缩略图宽度；`-1` 表示跟随全局配置 |
| `thumbnailHeight` | `int` | `-1` | 缩略图高度；`-1` 表示跟随全局配置 |
| `storage` | `UploadFile.StorageType` | `DEFAULT` | 底层存储实现；`DEFAULT` 表示跟随 `polo.storage.type` |
| `pathPrefix` | `String` | `""` | 存储路径前缀；为空时跟随全局配置 |
| `privateAccess` | `Toggle` | `DEFAULT` | 是否生成签名访问链接 |
| `signatureExpire` | `int` | `-1` | 签名链接有效期，单位秒；`-1` 表示跟随全局配置 |
| `filenameStrategy` | `FilenameStrategy` | `DEFAULT` | 文件名生成策略；`DEFAULT` 表示跟随全局配置 |

**`StorageType` 取值**

| 取值 | 含义 |
| --- | --- |
| `DEFAULT` | 跟随全局配置 |
| `LOCAL` | 本地存储 |
| `OSS` | 阿里云 OSS |
| `MINIO` | MinIO |
| `COS` | 腾讯云 COS |

**`FilenameStrategy` 取值**

| 取值 | 含义 |
| --- | --- |
| `DEFAULT` | 跟随全局配置 |
| `UUID` | 随机 UUID 文件名 |
| `TIMESTAMP` | 时间戳文件名 |
| `ORIGINAL` | 原始文件名（会做安全清洗） |
| `HASH` | 文件内容 MD5 |

**`Toggle` 取值**

| 取值 | 含义 |
| --- | --- |
| `DEFAULT` | 跟随全局配置 |
| `TRUE` | 显式开启 |
| `FALSE` | 显式关闭 |

**适用场景**

- 上传即返回文件信息
- 上传后立即写文件表
- 一个请求里同时上传并绑定业务主键

**什么时候不用它**

如果你还需要原始 `MultipartFile`，或者想自己控制“什么时候上传”，更适合改用：

```java
public Result<?> upload(@RequestParam("file") MultipartFile file) {
    UploadResult result = fileUploadService.upload(file, UploadOptions.builder().build());
    return Result.success(result);
}
```

---

## 4. web 模块

### 4.1 `@OperationLog`

**作用**

为方法添加操作日志记录能力。

**使用位置**

- 方法
- 组合注解

**示例**

```java
@OperationLog(module = "订单中心", type = OperationType.CREATE, desc = "创建订单", logResult = true)
@PostMapping("/orders")
public Result<?> create(@RequestBody OrderCreateRequest request) {
    return Result.success();
}
```

**字段说明**

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `module` | `String` | `"other"` | 操作所属模块 |
| `type` | `OperationType` | `OTHER` | 操作类型 |
| `desc` | `String` | `""` | 操作描述，支持 SpEL |
| `logParams` | `boolean` | `true` | 是否记录请求参数 |
| `paramMaskPatterns` | `String[]` | `{"password","token","secret","key"}` | 请求参数脱敏字段匹配规则 |
| `logResult` | `boolean` | `false` | 是否记录响应结果 |
| `resultMaskPatterns` | `String[]` | `{}` | 响应结果脱敏字段 |
| `async` | `boolean` | `true` | 是否异步记录 |
| `level` | `LogLevel` | `INFO` | 日志级别 |
| `ignoreExceptions` | `Class<? extends Exception>[]` | `{}` | 需要忽略记录的异常类型 |

**`OperationType` 取值**

| 取值 | 含义 |
| --- | --- |
| `CREATE` | 新增 |
| `UPDATE` | 修改 |
| `DELETE` | 删除 |
| `QUERY` | 查询 |
| `EXPORT` | 导出 |
| `IMPORT` | 导入 |
| `LOGIN` | 登录 |
| `LOGOUT` | 登出 |
| `AUDIT` | 审核 |
| `OTHER` | 其他 |

**`LogLevel` 取值**

| 取值 | 含义 |
| --- | --- |
| `DEBUG` | 调试日志 |
| `INFO` | 普通信息 |
| `WARN` | 警告日志 |
| `ERROR` | 错误日志 |

---

## 5. api-doc 模块

### 5.1 `@ApiOperation`

**作用**

为 Swagger / OpenAPI 文档补充接口说明。

**使用位置**

- 方法

**示例**

```java
@ApiOperation(value = "获取当前用户", description = "返回当前登录用户信息", tags = {"用户中心"})
@GetMapping("/me")
public Result<?> me() {
    return Result.success();
}
```

**字段说明**

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `value` | `String` | 无 | 接口摘要，对应 OpenAPI `summary` |
| `description` | `String` | `""` | 接口详细描述 |
| `tags` | `String[]` | `{}` | 接口所属标签 |

**说明**

- 当前 `@ApiOperation` 只负责文档
- 如果还要记录操作日志，请显式叠加 `@OperationLog`

---

## 6. mybatis-plus 模块

### 6.1 `@DataScope`

**作用**

为查询逻辑声明数据权限范围。

**使用位置**

- 类
- 方法

当前推荐标在 **Service 方法** 上。  
Mapper 方法上的 `@DataScope` 仍然可用，但更适合作为兜底或精确覆盖。

**示例**

```java
@DataScope(type = DataScopeType.DEPT_AND_CHILD, deptColumn = "o.dept_id")
public List<OrderEntity> pageOrders() {
    return orderMapper.selectList(new LambdaQueryWrapper<>());
}
```

**字段说明**

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `type` | `DataScopeType` | `DEPT_AND_CHILD` | 数据权限范围 |
| `deptColumn` | `String` | `"dept_id"` | 部门字段名，用于 SQL 拼接 |
| `userColumn` | `String` | `"create_by"` | 创建者字段名 |
| `customCondition` | `String` | `""` | 自定义 SQL 条件 |

**`DataScopeType` 取值**

| 取值 | 含义 |
| --- | --- |
| `ALL` | 全部数据 |
| `DEPT_ONLY` | 仅本部门 |
| `DEPT_AND_CHILD` | 本部门及子部门 |
| `SELF_ONLY` | 仅本人创建 |
| `CUSTOM` | 自定义规则 |

**补充说明**

- service 方法上的 `@DataScope` 会先经过 AOP 写入上下文
- 后续 MyBatis 查询执行时再由拦截器读取上下文并改写 SQL
- 如果你的查询运行在异步线程里，默认不会自动继承当前数据权限上下文

---

### 6.2 `@AutoFillField`

**作用**

标记实体字段的自动填充语义。

**使用位置**

- 字段

**示例**

```java
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;

public class OrderEntity {

    @TableField(fill = FieldFill.INSERT)
    @AutoFillField(type = AutoFillType.CREATE_TIME)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    @AutoFillField(type = AutoFillType.CREATE_BY)
    private Long creatorId;
}
```

**字段说明**

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `type` | `AutoFillType` | 无 | 当前字段自动填充类型 |

**`AutoFillType` 取值**

| 取值 | 含义 |
| --- | --- |
| `CREATE_TIME` | 创建时间 |
| `UPDATE_TIME` | 更新时间 |
| `CREATE_BY` | 创建人 |
| `UPDATE_BY` | 更新人 |
| `TENANT_ID` | 租户 ID |
| `DEPT_ID` | 部门 ID |

**补充说明**

- 当前推荐把 `@AutoFillField` 标在 entity 字段上
- 为了让 MyBatis-Plus 稳定触发自动填充，建议同一字段再配合 `@TableField(fill = ...)`
- request DTO 不需要写这个注解，因为自动填充最终发生在交给 mapper 的 entity 上

---

## 7. validation 模块

### 7.1 通用说明

`validation` 模块里的格式校验注解大多基于 Bean Validation，自带这些标准字段：

| 字段 | 含义 |
| --- | --- |
| `message` | 校验失败提示 |
| `groups` | 校验分组 |
| `payload` | 扩展载荷 |

下面只重点解释每个注解额外提供的业务字段。

---

### 7.2 `@Phone`

**作用**

校验手机号格式。

**使用位置**

- 字段
- 方法参数

**示例**

```java
@Phone(required = true)
private String mobile;
```

**字段说明**

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `required` | `boolean` | `true` | 是否必填 |

---

### 7.3 `@Email`

**作用**

校验邮箱格式，可附加域名限制。

**示例**

```java
@Email(allowNull = false, allowedDomains = {"qq.com", "163.com"})
private String email;
```

**字段说明**

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `allowNull` | `boolean` | `false` | 是否允许为空 |
| `allowedDomains` | `String[]` | `{}` | 域名白名单，空表示不限制 |
| `deniedDomains` | `String[]` | `{"tempmail.com","10minutemail.com"}` | 域名黑名单 |
| `checkMx` | `boolean` | `false` | 是否检查 MX 记录 |

---

### 7.4 `@Password`

**作用**

校验密码强度。

**示例**

```java
@Password(min = 8, requireUpper = true, requireSpecial = true, minStrength = 3)
private String password;
```

**字段说明**

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `min` | `int` | `8` | 最小长度 |
| `max` | `int` | `32` | 最大长度 |
| `requireDigit` | `boolean` | `true` | 是否要求数字 |
| `requireLower` | `boolean` | `true` | 是否要求小写字母 |
| `requireUpper` | `boolean` | `false` | 是否要求大写字母 |
| `requireSpecial` | `boolean` | `false` | 是否要求特殊字符 |
| `specialChars` | `String` | 常用特殊字符集合 | 特殊字符集合 |
| `notContainUsername` | `boolean` | `true` | 是否禁止包含用户名 |
| `noSequential` | `boolean` | `true` | 是否禁止连续字符 |
| `minStrength` | `int` | `2` | 最低强度等级 |

---

### 7.5 `@IdCard`

**作用**

校验身份证号。

**示例**

```java
@IdCard(allow15 = true, strictCheck = false)
private String idCard;
```

**字段说明**

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `allow15` | `boolean` | `true` | 是否允许 15 位老身份证 |
| `strictCheck` | `boolean` | `false` | 是否校验行政区划代码 |

---

### 7.6 `@BankCard`

**作用**

校验银行卡号格式。

**示例**

```java
@BankCard
private String bankCardNo;
```

这个注解没有额外业务字段，只使用标准的 `message / groups / payload`。

---

### 7.7 `@CreditCode`

**作用**

校验统一社会信用代码格式。

**示例**

```java
@CreditCode
private String creditCode;
```

这个注解没有额外业务字段，只使用标准的 `message / groups / payload`。

---

### 7.8 `@CarNo`

**作用**

校验车牌号。

**示例**

```java
@CarNo(type = CarNo.CarType.NEW_ENERGY)
private String carNo;
```

**字段说明**

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `type` | `CarNo.CarType` | `AUTO` | 车牌类型 |

**`CarType` 取值**

| 取值 | 含义 |
| --- | --- |
| `AUTO` | 自动识别 |
| `NEW_ENERGY` | 新能源车牌 |
| `NORMAL` | 普通车牌 |
| `HONG_KONG` | 港牌 |
| `MACAO` | 澳牌 |
| `ARMY` | 军牌 |
| `POLICE` | 警牌 |

---

### 7.9 `@IPAddress`

**作用**

校验 IP 地址格式。

**示例**

```java
@IPAddress(version = {IPAddress.IPVersion.V4}, allowPrivate = false)
private String ip;
```

**字段说明**

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `version` | `IPAddress.IPVersion[]` | `{V4, V6}` | 允许的 IP 版本 |
| `allowPrivate` | `boolean` | `true` | 是否允许私有地址 |
| `allowLoopback` | `boolean` | `true` | 是否允许回环地址 |

**`IPVersion` 取值**

| 取值 | 含义 |
| --- | --- |
| `V4` | IPv4 |
| `V6` | IPv6 |

---

### 7.10 `@InputContent`

**作用**

做文本内容安全校验，支持敏感词、正则规则和 AI 检测。

**使用位置**

- 字段
- 方法参数
- 方法

**示例**

```java
@InputContent(
        types = {
                InputContent.CheckType.SENSITIVE_WORD,
                InputContent.CheckType.REGEX_PATTERN
        },
        strategy = InputContent.Strategy.REJECT,
        checkContactLeak = true
)
private String content;
```

**字段说明**

| 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `types` | `CheckType[]` | `{SENSITIVE_WORD}` | 检测类型 |
| `categoryCodes` | `String[]` | `{}` | 推荐使用的分类编码，支持用户自定义分类 |
| `strategy` | `Strategy` | `REJECT` | 命中后的处理策略 |
| `maskChar` | `String` | `"*"` | `MASK` 策略下的替换字符 |
| `customWords` | `String[]` | `{}` | 额外自定义敏感词 |
| `ignoreWords` | `String[]` | `{}` | 白名单词 |
| `checkContactLeak` | `boolean` | `true` | 是否检查联系方式泄露 |
| `enableAiCheck` | `boolean` | `false` | 是否启用 AI 检测 |
| `aiThreshold` | `double` | `0.8` | AI 检测阈值 |

**`CheckType` 取值**

| 取值 | 含义 |
| --- | --- |
| `SENSITIVE_WORD` | 敏感词匹配 |
| `REGEX_PATTERN` | 正则规则匹配 |
| `SEMANTIC_ANALYSIS` | AI 语义分析 |
| `CUSTOM_RULE` | 自定义规则 |

**`Strategy` 取值**

| 取值 | 含义 |
| --- | --- |
| `REJECT` | 直接拒绝 |
| `MASK` | 脱敏替换 |
| `REPLACE` | 智能替换 |
| `REVIEW` | 标记人工审核 |
| `LOG` | 只记录日志 |

**推荐内置分类编码**

| 取值 | 含义 |
| --- | --- |
| `POLITICS` | 政治敏感 |
| `PORNOGRAPHY` | 色情低俗 |
| `VIOLENCE` | 暴力恐怖 |
| `DISCRIMINATION` | 歧视辱骂 |
| `GAMBLING` | 赌博诈骗 |
| `DRUGS` | 毒品违禁 |
| `PRIVACY` | 隐私信息 |
| `ADVERTISING` | 广告垃圾 |

也可以直接写自定义分类，例如：

```java
@InputContent(categoryCodes = {"RUMOR", "SCAM"})
private String content;
```

**适用场景**

- 帖子内容
- 评论内容
- 用户昵称
- 留言
- 简介

---

## 8. 使用建议

如果你刚开始接这个项目，建议优先掌握这些注解：

1. `@CurrentUser`
2. `@CurrentUserAttribute`
3. `@UploadFile`
4. `@RequireRole`
5. `@RequirePermission`
6. `@OperationLog`
7. `@ApiOperation`
8. `@DataScope`
9. `@AutoFillField`
10. `@InputContent`

这几类基本覆盖了：

- 当前用户获取
- 权限控制
- 操作日志
- 文档说明
- 数据权限
- 自动填充
- 内容安全
