# polo-boot-storage

## 模块作用

`polo-boot-storage` 负责文件上传、下载、删除和分片续传，当前默认使用 MinIO 演示，同时支持本地存储、阿里云 OSS 和腾讯云 COS。

它现在分成两层：

- 底层 `FileStorage`
  只负责把对象写进 MinIO / 本地 / OSS / COS，以及读取元信息、范围下载、删除对象。
- 上层 `FileUploadService`
  负责文件校验、文件名生成、缩略图、签名 URL、删除编排、`UploadResult` 组装。

适合场景：

- 头像上传
- 附件上传
- 富文本图片上传
- 后台文件中心
- 上传后马上把文件信息记录到数据库
- 统一管理“全局默认上传规则 + 接口局部覆盖”
- 大文件分片断点续传
- HTTP Range 下载 / 断点下载

## 关键类

- [`StorageAutoConfiguration.java`](../../polo-boot-storage/src/main/java/com/polo/boot/storage/config/StorageAutoConfiguration.java)
- [`StorageProperties.java`](../../polo-boot-storage/src/main/java/com/polo/boot/storage/properties/StorageProperties.java)
- [`FileStorage.java`](../../polo-boot-storage/src/main/java/com/polo/boot/storage/service/FileStorage.java)
- [`FileUploadService.java`](../../polo-boot-storage/src/main/java/com/polo/boot/storage/service/FileUploadService.java)
- [`UploadFile.java`](../../polo-boot-storage/src/main/java/com/polo/boot/storage/annotation/UploadFile.java)
- [`UploadOptions.java`](../../polo-boot-storage/src/main/java/com/polo/boot/storage/model/UploadOptions.java)
- [`DefaultFileUploadService.java`](../../polo-boot-storage/src/main/java/com/polo/boot/storage/service/impl/DefaultFileUploadService.java)
- [`UploadResult.java`](../../polo-boot-storage/src/main/java/com/polo/boot/storage/model/UploadResult.java)
- [`UploadContext.java`](../../polo-boot-storage/src/main/java/com/polo/boot/storage/context/UploadContext.java)
- [`ChunkUploadService.java`](../../polo-boot-storage/src/main/java/com/polo/boot/storage/service/ChunkUploadService.java)
- [`ChunkUploadInitRequest.java`](../../polo-boot-storage/src/main/java/com/polo/boot/storage/model/ChunkUploadInitRequest.java)（内部辅助类，推荐直接使用基本参数）
- [`ChunkUploadState.java`](../../polo-boot-storage/src/main/java/com/polo/boot/storage/model/ChunkUploadState.java)
- [`UploadFileArgumentResolver.java`](../../polo-boot-storage/src/main/java/com/polo/boot/storage/resolver/UploadFileArgumentResolver.java)
- [`MinioFileStorage.java`](../../polo-boot-storage/src/main/java/com/polo/boot/storage/service/impl/MinioFileStorage.java)

## 接入方式

```xml
<dependency>
    <groupId>io.github.smiling11123</groupId>
    <artifactId>polo-boot-storage</artifactId>
    <version>0.1.1</version>
</dependency>
```

## 配置项

```yaml
polo:
  storage:
    enabled: true
    minio-enabled: true
    type: MINIO
    endpoint: http://127.0.0.1:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket: demo
    public-url: http://127.0.0.1:9000
    cos:
      cos-enabled: false
      region: ap-shanghai
      bucket: demo-1250000000
      secret-id: your-secret-id
      secret-key: your-secret-key
      public-url: https://demo-1250000000.cos.ap-shanghai.myqcloud.com
    upload:
      path-prefix: uploads
      max-size: 10MB
      allowed-extensions:
        - jpg
        - jpeg
        - png
        - pdf
      allowed-types:
        - image/jpeg
        - image/png
        - application/pdf
      filename-strategy: UUID
      private-access: false
      signature-expire: 3600
      generate-thumbnail: false
      thumbnail-width: 200
      thumbnail-height: 200
    delete:
      delete-thumbnail-on-delete: false
      allow-bucket-delete: false
    chunk:
      enabled: true
      temp-dir: D:/data/upload-chunks
      delete-after-complete: true
      merge-buffer-size: 1048576
```

配置说明：

- `enabled`：是否启用 storage 模块
- `type`：默认底层存储类型，当前常用 `MINIO`
- `minio-enabled`：是否启用默认 MinIO 实现
- `cos.cos-enabled`：是否启用默认腾讯云 COS 实现
- `cos.region`：COS 所属地域
- `cos.bucket`：COS 存储桶名称
- `cos.secret-id` / `cos.secret-key`：腾讯云访问凭证
- `cos.public-url`：COS 对外访问地址前缀
- `endpoint`：MinIO 服务地址
- `access-key` / `secret-key`：MinIO 账号信息
- `bucket`：默认文件桶
- `public-url`：对外访问 URL
- `upload.path-prefix`：全局默认上传目录
- `upload.max-size`：全局默认单文件大小限制，支持 `10MB`、`512KB`、`1GB` 这类更友好的写法
- `upload.allowed-extensions` / `upload.allowed-types`：全局默认白名单
- `upload.filename-strategy`：全局默认文件名生成策略
- `upload.private-access`：全局默认是否走签名访问
- `upload.signature-expire`：全局默认签名链接过期秒数
- `upload.generate-thumbnail`：全局默认是否生成缩略图
- `upload.thumbnail-width` / `upload.thumbnail-height`：全局默认缩略图尺寸
- `delete.delete-thumbnail-on-delete`：删除原文件时是否同时尝试删除缩略图，默认关闭
- `delete.allow-bucket-delete`：是否允许调用删除第三方存储桶能力，默认关闭
- `chunk.enabled`：是否启用分片断点续传服务
- `chunk.temp-dir`：分片临时目录
- `chunk.delete-after-complete`：完成上传后是否自动删除本地分片
- `chunk.merge-buffer-size`：分片合并缓冲区大小

完整的带中文注释配置示例：

- [`application-example.yml`](../../demo/src/main/resources/application-example.yml)

默认规则优先级：

1. 手动传入的 `UploadOptions`
2. `@UploadFile` 注解局部覆盖
3. `polo.storage.upload.*` 全局默认配置

这意味着：

- 绝大多数上传接口只需要配置全局默认值
- 某个接口有特殊限制时，再在 `@UploadFile` 或 `UploadOptions` 上局部覆盖

## 使用示例

### 1. 参数解析阶段自动上传

```java
@PostMapping("/upload")
public Result<UploadResult> upload(
        @UploadFile(value = "file", pathPrefix = "avatars")
        UploadResult uploadResult) {
    uploadResult.setBizType("user-avatar");
    uploadResult.setBizId(1001L);
    fileRecordService.save(uploadResult);
    return Result.success(uploadResult);
}
```

这里要注意一件事：

- 参数类型是 `UploadResult`
- 文件本体来自 `multipart/form-data` 请求里的 `file` 字段
- 参数解析器会先取出 `MultipartFile`，完成上传后再把 `UploadResult` 注入给 controller
- `fieldName` 表示“这次上传结果属于哪个表单字段”，主要用于 `UploadContext` 分组，不直接决定最终存储路径

所以这条链更适合：

- 上传完成后直接落库
- 上传完成后立刻绑定业务 ID
- 只关心上传结果，不再直接操作原始文件对象

这里没有显式写 `maxSize / filenameStrategy / privateAccess`，就会自动回退到 `application.yml` 里的默认上传配置。

这时文件已经在参数解析阶段上传完成，业务方法拿到的是结构化结果：

- `uploadId`
- `fieldName`
- `originalFilename`
- `filename`
- `filepath`
- `url`
- `signedUrl`
- `thumbnailUrl`
- `size`
- `contentType`
- `fileMd5`
- `storageType`
- `metadata`

所以你可以直接：

- 记录文件表
- 绑定业务主键
- 回写 `bizType / bizId`
- 发消息或做审计
限制：

- 文件上传发生在参数解析阶段，不适合作为复杂测试入口
- 参数解析发生在部分切面之前，因此不适合承载强依赖 AOP 的上传前逻辑
- Swagger 对这种“请求里传文件、方法里接收结果对象”的展示不够友好，演示和联调用例更推荐手动上传接口

### 2. 手动上传：更适合上传后立刻落库

```java
private final FileUploadService fileUploadService;

@PostMapping("/upload/manual")
public Result<UploadResult> uploadManual(@RequestParam("file") MultipartFile file) {
    UploadResult uploadResult = fileUploadService.upload(file, UploadOptions.builder()
        .fieldName("file")
        .pathPrefix("manual")
        .build());
    fileRecordService.save(uploadResult);
    return Result.success(uploadResult);
}
```

这种写法的优点是：

- 上传和业务流程都在你自己控制里
- 更适合先上传、再落库、再绑定业务 ID
- 不依赖参数解析器
- 需要原始 `MultipartFile` 时更直观

如果这里不传 `maxSize / filenameStrategy / privateAccess`，同样会走全局默认配置。

### 3. 在 service 里继续拿上传结果

如果文件已经通过 `@UploadFile` 上传了，但你想在 service 层继续处理，也可以读取当前请求的上传上下文：

```java
UploadResult uploadResult = UploadContext.getFirst("file")
        .orElseThrow(() -> new BizException(30000, "缺少上传结果"));
fileRecordService.save(uploadResult);
```

如果一个请求里有多个上传字段，也可以：

```java
Map<String, List<UploadResult>> allResults = UploadContext.getResultsByField();
```

适用前提：

- 仍然在当前 HTTP 请求线程内
- 更适合同步链路里的 controller / service 继续处理上传结果
- 如果要跨异步线程继续使用，请配合 `polo-boot-context-trans`
- 即使开启了透传，也更推荐把关键文件标识显式传给长耗时任务

### 4. demo 示例

- [`UploadController.java`](../../demo/src/main/java/com/polo/demo/controller/UploadController.java)

当前 demo 包含三种入口：

- `/files/upload`
- `/files/upload/manual`
- `/files/upload/context`

### 5. 下载文件

现在的下载入口是：

- `GET /files/download`

支持参数：

- `filepath`：存储相对路径，必填
- `storageType`：存储类型，可选；当文件不在当前默认存储里时建议显式传入，例如 `minio`、`oss`、`cos`、`local`

当前下载能力有两种模式：

- 普通下载：不带 `Range` 请求头，返回完整文件
- 分片下载：带标准 HTTP `Range` 请求头，返回 `206 Partial Content`

也就是说，浏览器、下载器、断点续传客户端都可以直接复用标准 Range 机制，不需要再额外定义一套“自定义下载分片协议”。

例如：

```http
GET /files/download?filepath=demo/report.pdf
Range: bytes=0-1048575
```

服务端会：

- 校验文件是否存在
- 读取底层存储里的真实文件大小和 MIME 类型
- 解析 `Range`
- 返回 `Accept-Ranges: bytes`
- 按请求范围输出对应字节流

当前底层的 `LOCAL / MINIO / OSS / COS` 都已经支持范围读取。

### 6. 删除文件

现在的删除入口是：

- `DELETE /files/delete`

支持参数：

- `filepath`：存储相对路径，必填
- `storageType`：存储类型，可选

删除逻辑现在做了两件事：

- 删除原文件
- 按真实 `filepath` 推导缩略图路径，并在 `polo.storage.delete.delete-thumbnail-on-delete=true` 时才尝试连带删除

这里不再依赖调用方额外再传一个 `pathPrefix`，因为缩略图路径已经可以从原始文件路径稳定推导出来，例如：

- 原文件：`demo/a.png`
- 缩略图：`demo/thumbs/a.png.jpg`

删除返回的是 [`OperationResult.java`](../../polo-boot-storage/src/main/java/com/polo/boot/storage/model/OperationResult.java)，里面会带：

- 原文件路径
- 存储类型
- 文件大小、类型
- 是否开启了缩略图联动删除
- 缩略图是否存在、是否删除成功

如果原文件不存在，现在会直接按“数据不存在”处理，而不是静默返回成功。

### 7. 删除第三方存储桶

现在额外提供了一个删除当前存储实现 bucket 的能力：

- `DELETE /files/bucket`

支持参数：

- `storageType`：存储类型，可选；不传时使用当前默认存储

这个能力默认是关闭的，只有在下面配置开启后才允许执行：

```yaml
polo:
  storage:
    delete:
      allow-bucket-delete: true
```

当前支持 bucket 删除的默认实现有：

- MinIO
- OSS
- COS

本地存储不支持 bucket 删除。  
另外需要注意，大部分对象存储要求 bucket 为空才能删除；如果 bucket 里还有对象，SDK 删除通常会失败。

## 分片断点续传

当前实现采用“本地临时分片 + 最终复用 `FileUploadService` 上传”的方式，所以：

- MinIO
- OSS
- COS
- LOCAL

都可以共用这一套分片流程。

完整流程：

1. 调 `POST /files/chunk/init` 初始化上传会话，直接传 `fieldName`、`originalFilename`、`contentType`、`totalSize`、`totalChunks`
2. 前端保存返回的 `uploadId`
3. 逐片调用 `POST /files/chunk/part`
4. 中途断开时，调用 `GET /files/chunk/status` 查询已上传分片
5. 补传缺失分片
6. 全部分片上传完后，调用 `POST /files/chunk/complete`
7. 如果用户取消上传，调用 `DELETE /files/chunk/abort`

现在也支持“首个分片自动初始化”简化模式：

1. 第一个分片直接调用 `POST /files/chunk/part`
2. 不传 `uploadId`
3. 额外传 `originalFilename`、`totalSize`、`totalChunks`
4. 要求 `chunkIndex=0`
5. 服务端会自动创建上传会话，并在返回的 `ChunkUploadState` 里带上新的 `uploadId`
6. 后续分片继续带这个 `uploadId`

也就是说，调用方可以二选一：

- 显式模式：先 `init`，再逐片上传
- 简化模式：首个分片自动初始化，后续按返回的 `uploadId` 继续

显式模式下的 `init` 接口不需要调用方自己组装内部 DTO，直接传基本类型参数即可。

当前 demo 约定：

- `chunkIndex` 从 `0` 开始
- `complete` 阶段仍然支持传 `pathPrefix`
- 最终返回的仍然是统一的 `UploadResult`

当前实现的会话状态：

- `INIT`：会话已创建，还没有成功写入任何分片
- `UPLOADING`：正在上传分片
- `COMPLETING`：正在合并分片并上传到最终存储
- `COMPLETED`：上传已经完成
- `ABORTED`：上传已取消

当前实现的并发与可靠性策略：

- 同一个 `uploadId` 下的 `uploadChunk / complete / abort` 会串行执行，避免同一会话里的写分片、合并、取消互相踩踏
- 不同 `uploadId` 之间可以并发上传
- 单个分片会先写到 `*.part.tmp`，写完后再原子移动成 `*.part`，避免半截分片被误判为上传成功
- 合并分片过程中会持续检查会话状态，如果会话已经被标记为 `ABORTED`，会在下一个检查点终止
- `abort` 不是简单删除目录，而是先标记会话为 `ABORTED`；如果当前没有正在执行的分片/合并任务，会立刻清理目录；如果有任务正在执行，会在任务退出时清理

这意味着：

- “暂停上传”不需要专门接口，前端停止继续发送后续分片即可
- “继续上传”时，先查 `GET /files/chunk/status`，根据 `uploadedChunks` 只补传缺失分片
- “取消上传”时，调用 `DELETE /files/chunk/abort`
- 调用方不想单独先调 `init` 时，可以在首个分片请求里自动初始化，但拿到返回的 `uploadId` 后仍然要保存，后续状态查询、补传、完成合并、取消上传都依赖它

需要注意的边界：

- 浏览器取消某一个分片请求，并不等于服务端一定瞬间停止；如果该分片已经被服务端完整接收，它仍然可能成功落盘
- `abort` 对正在执行的分片写入和本地合并阶段是协作式生效的，会在下一次状态检查时退出
- 如果最终对象存储上传已经开始，当前实现只能尽量在上传前或上传后检查并中止流程，不能保证对底层存储 SDK 做到绝对抢占式中断
- 当前分片状态保存在本地临时目录，更适合同机部署；如果是多实例部署，建议共享临时目录，或者把会话路由到同一实例
- 目前还没有分片级 MD5/sha256 校验，如果你对完整性要求很高，建议继续加“分片校验值 + 最终文件校验值”

适合场景：

- 大文件上传
- 网络波动较大的上传场景
- 需要前端做断点续传或秒传前置校验的场景

## 自定义实现

如果你不想用默认 MinIO，而想接自己的对象存储，可以自己提供 `FileStorage` Bean。

最小实现只需要这几个方法：

```java
public class CustomFileStorage implements FileStorage {

    @Override
    public String getType() {
        return "custom";
    }

    @Override
    public String upload(InputStream inputStream, long size, String filepath, String contentType) {
        ...
    }

    @Override
    public boolean delete(String filepath) {
        ...
    }

    @Override
    public boolean exists(String filepath) {
        ...
    }
}
```

如果要支持私有文件临时访问，再额外实现：

```java
@Override
public String generateSignedUrl(String filepath, int expireSeconds) {
    ...
}
```

## 当前支持的两种接入思路

### 自动上传

- 适合 controller 想直接拿 `UploadResult`
- 适合统一上传入口
- 适合简单落库和返回前端
- 更适合“上传即消费”场景

### 手动上传

- 适合复杂业务流程
- 适合先拿业务 ID，再组织路径和元数据
- 适合 service 主导上传逻辑
- 适合既要原始文件对象，又要自己决定何时上传

## 建议

- 想“上传后马上落库”，优先用 `@UploadFile UploadResult`
- 想“上传过程和业务流程完全自己控制”，优先用 `FileUploadService`
- 需要 service 层复用上传结果时，用 `UploadContext`
