# demo

`demo` 是 `polo-boot-starter` 的可运行示例工程，用来把各模块能力串起来做完整演示。

如果你是第一次接触这个项目，建议先跑 demo，再回头按模块阅读源码和文档。

如果你想直接做前后端联调，仓库里还提供了一个 Vue 3 测试工作台：

- [../demo-FrontEnd/README.md](../demo-FrontEnd/README.md)

## demo 覆盖了什么

- 登录、刷新 token、设备管理、下线设备
- 当前用户上下文与角色 / 权限控制
- 文件上传、下载、删除、分片断点续传
- 异步上下文透传
- MyBatis-Plus 租户隔离、数据权限、审计、自动填充、乐观锁
- 参数校验、内容安全、动态词库管理
- 限流、防重复提交、异常处理
- OpenAPI / Swagger 分组展示
- 二维码登录前后端联调
- Vue 3 文件上传下载 / 二维码登录测试工作台

## 运行环境

建议本地准备：

- JDK 21
- Maven 3.9+
- Redis
- MinIO

补充说明：

- demo 默认使用内存 H2，启动时会自动初始化表结构和示例数据
- 如果只体验登录、Swagger、用户上下文、权限、多设备，Redis 是必须的
- 如果要测试文件上传，MinIO 需要可用

## 启动方式

先在根目录编译：

```bash
mvn -q -DskipTests compile
```

再运行：

- `demo/src/main/java/com/polo/demo/DemoApplication.java`

或者：

```bash
mvn -pl demo spring-boot:run
```

如果还要启动前端测试页，再在仓库根目录另开一个终端进入 `demo-FrontEnd`：

```bash
cd demo-FrontEnd
npm install
npm run dev
```

## 默认信息

- 端口：`8080`
- 账号：`admin / 123456`
- 账号：`manager / 123456`
- 账号：`auditor / 123456`
- 账号：`user / 123456`

配置文件：

- [`src/main/resources/application.yml`](./src/main/resources/application.yml)
- [`src/main/resources/application-example.yml`](./src/main/resources/application-example.yml)

前端测试页说明：

- [`../demo-FrontEnd/README.md`](../demo-FrontEnd/README.md)

## Swagger

- Swagger UI：[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- 全量文档 JSON：[http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- 分组文档 JSON：`/v3/api-docs/{group}`

默认分组：

- 全部接口
- Authentication
- User APIs
- File APIs
- Validation APIs
- Context Transfer APIs
- MyBatis Demo APIs
- Exception APIs

说明：

- `/swagger-ui/index.html` 才是页面入口
- `/v3/api-docs` 和 `/v3/api-docs/{group}` 是 OpenAPI JSON 数据源
- demo 默认开启了 `polo.api-doc.persist-authorization=true`，切换分组后 Bearer Token 会继续保留

## 推荐体验顺序

1. `POST /auth/login`
2. `GET /user/me`
3. `GET /user/context`
4. `POST /files/upload/manual`
5. `GET /context-trans/async/user`
6. `GET /mybatis-demo/overview`
7. `POST /mybatis-demo/records`
8. `PUT /mybatis-demo/records/{id}`
9. `POST /validate/dynamic-words`
10. `GET /rate-limit/global`

## demo 里可以直接体验什么

### 认证相关

- `POST /auth/login`
- `POST /auth/refresh`
- `GET /auth/qrcode/generate`
- `GET /auth/qrcode/check`
- `POST /auth/qrcode/scan`
- `POST /auth/qrcode/confirm`
- `GET /auth/devices`
- `POST /auth/logout`
- `POST /auth/logout-all`
- `DELETE /auth/devices/{sessionId}`

### 用户与权限相关

- `GET /user/me`
- `GET /user/context`
- `GET /user/list`
- `GET /user/permission-demo`

### 文件相关

- `POST /files/upload`
- `POST /files/upload/manual`
- `POST /files/upload/context`
- `GET /files/download`
- `DELETE /files/delete`
- `DELETE /files/bucket`
- `POST /files/chunk/init`
- `POST /files/chunk/part`
- `GET /files/chunk/status`
- `POST /files/chunk/complete`
- `DELETE /files/chunk/abort`

### 上下文透传相关

- `GET /context-trans/async/user`
- `GET /context-trans/async/data-scope`
- `POST /context-trans/async/upload`

### MyBatis 能力演示

- `GET /mybatis-demo/accounts`
- `GET /mybatis-demo/overview`
- `GET /mybatis-demo/raw/all`
- `GET /mybatis-demo/tenant/list`
- `GET /mybatis-demo/tenant/page`
- `GET /mybatis-demo/scope/dept`
- `GET /mybatis-demo/scope/self`
- `GET /mybatis-demo/scope/custom-approved`
- `GET /mybatis-demo/records/{id}/audit`
- `POST /mybatis-demo/records`
- `PUT /mybatis-demo/records/{id}`
- `PUT /mybatis-demo/records/{id}/self`
- `POST /mybatis-demo/records/{id}/optimistic-lock-conflict`

### 校验、词库与其他基础能力

- `GET /validate/phone`
- `GET /validate/email`
- `POST /validate/content`
- `POST /validate/full`
- `GET /validate/dynamic-words`
- `POST /validate/dynamic-words`
- `DELETE /validate/dynamic-words`
- `POST /validate/dynamic-words/refresh`
- `GET /rate-limit/global`
- `GET /rate-limit/user`
- `POST /rate-limit/submit`
- `POST /rate-limit/submit-immediate`
- `GET /exception/biz`
- `GET /exception/system`
- `POST /exception/validation`

## 测试资源

- [Demo 接口测试清单](../docs/demo-api-test-checklist.md)
- [前端 Demo 使用说明](../demo-FrontEnd/README.md)

## 建议

如果你是第一次接这个仓库，最稳的方式是：

1. 先跑通 demo
2. 再按模块查看文档
3. 最后挑你真正需要的模块接入业务项目
