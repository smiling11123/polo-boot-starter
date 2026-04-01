# polo-boot-starter

`polo-boot-starter` 是一个面向 Spring Boot 3 的多模块后端 starter 项目。

它的目标不是做一个“大而全的框架”，而是把后台项目里高频重复的基础设施能力拆成可按需组合的模块，让业务项目可以：

- 按需引入
- 自动装配
- 配置清晰
- 默认可用
- 保留扩展空间

## 项目定位

这个项目适合：

- 想快速搭一套 Spring Boot 后端基础设施
- 不想每个新项目都重复写统一返回、异常处理、JWT、Swagger、审计字段
- 希望功能按模块拆开接入，而不是一次性引入整套框架
- 希望保留业务自由度，同时复用成熟公共能力

## 模块总览

根工程是 Maven 聚合工程，真正接入时请按模块选择依赖。

| 模块 | 作用 | 常见使用场景 |
| --- | --- | --- |
| `polo-boot-core` | 统一返回体、错误码、业务异常、公共上下文契约 | 所有项目都建议引入 |
| `polo-boot-web` | 全局异常处理、操作日志 | Web API 项目 |
| `polo-boot-security` | JWT、会话、多设备、角色/权限、用户上下文注入、防重复提交、限流 | 需要登录鉴权的项目 |
| `polo-boot-context-trans` | 异步线程上下文透传、默认线程池装配 | 需要 `@Async`、线程池任务或异步日志的项目 |
| `polo-boot-cache` | RedisService 封装与自动装配 | 需要 Redis 缓存的项目 |
| `polo-boot-storage` | MinIO / OSS / COS / 本地文件上传、下载、删除、分片续传 | 文件上传、对象存储、断点续传 |
| `polo-boot-validation` | 自定义校验注解、内容安全校验 | 参数校验、文本内容审核 |
| `polo-boot-mybatis-plus` | MyBatis-Plus 分页、乐观锁、租户、数据权限、自动填充 | 使用 MyBatis-Plus 的业务系统 |
| `polo-boot-api-doc` | OpenAPI / Swagger 自动装配、分组、默认响应 | 接口文档展示 |
| `demo` | 可直接运行的示例工程 | 第一次了解项目时优先看这个 |

## 你会得到哪些能力

- 统一返回体 `Result`
- 业务异常 `BizException`
- 全局异常处理
- 操作日志注解与切面
- JWT 鉴权
- 单 token / 双 token / refresh / 会话模式 / 多设备
- `@CurrentUser` / `@CurrentUserAttribute`
- `@RequireRole` / `@RequirePermission`
- 防重复提交、接口限流
- 异步线程上下文透传
- RedisService 封装
- OpenAPI / Swagger 自动装配与接口分组
- 文件上传、下载、删除、分片续传
- 全局默认上传配置 + `@UploadFile` 局部覆盖
- 自定义校验注解
- 文本内容安全校验与动态词库
- MyBatis-Plus 分页、乐观锁、租户隔离、数据权限、审计自动填充

## 技术基线

- Java 21
- Spring Boot 3.3.5
- Spring MVC
- Spring Data Redis
- MyBatis-Plus 3.5.5
- SpringDoc OpenAPI 2.3.0
- JJWT 0.12.3

## 快速开始

编译项目：

```bash
mvn -q -DskipTests compile
```

第一次体验建议直接运行 demo，再按模块回看源码和文档：

- [demo/README.md](./demo/README.md)

## 文档导航

- [模块使用说明目录](./docs/modules/README.md)
- [扩展点与自定义接口手册](./docs/extension-points.md)
- [项目自定义注解使用手册](./docs/annotation-principles.md)
- [全量示例配置（含中文注释）](./demo/src/main/resources/application-example.yml)
- [Demo 运行与测试说明](./demo/README.md)
- [Demo 接口测试清单（Postman / Apifox）](./docs/demo-api-test-checklist.md)
- [Postman Collection 与环境模板](./docs/postman/README.md)
- [内容安全校验使用说明](./docs/validation-content-security.md)
- [Redis 动态词库初始化脚本](./docs/redis/content-security-words.redis)

## 推荐阅读顺序

1. 先看 [demo/README.md](./demo/README.md)
2. 再看 [docs/modules/README.md](./docs/modules/README.md)
3. 接着按 `core -> web -> security -> context-trans -> storage -> validation -> mybatis-plus` 阅读
4. 最后再回到 demo 对照接口和配置理解整条链路

## 注意

1. 本项目部分功能开发和测试未完成，项目暂时还是个半成品
2. 修改、使用、二次分发请注明出处