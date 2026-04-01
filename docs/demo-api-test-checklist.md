# Demo 接口测试清单

这份清单按“先登录，再分模块验证”的顺序整理，适合用 Swagger、Postman 或 Apifox 跑一遍 demo。

## 1. 启动前准备

- Redis 可用：`localhost:6379`
- MinIO 可用：`http://127.0.0.1:9000`
- H2 不需要单独安装，demo 启动时会自动初始化内存库

## 2. 推荐测试账号

- `admin / 123456`
  说明：超管，拥有全部权限，适合测原始数据、全部数据权限和 bucket 删除
- `manager / 123456`
  说明：部门经理，适合测部门及子部门数据权限、创建、更新、分页
- `auditor / 123456`
  说明：审计员，适合测审计字段查看和部门只读访问
- `user / 123456`
  说明：普通用户，适合测 `SELF_ONLY` 和“只能更新自己数据”

## 3. 鉴权主链

先登录：

- `POST /auth/login`

说明：

- Swagger UI 页面入口是 `/swagger-ui/index.html`
- `/v3/api-docs` 和 `/v3/api-docs/{group}` 是 JSON 数据源，不是页面
- demo 默认开启了 `persist-authorization`，切换分组后不需要重新绑定 token；如果浏览器里还是丢 token，先强刷页面再重新 `Authorize` 一次

再测：

- `POST /auth/refresh`
- `GET /auth/devices`
- `POST /auth/logout`
- `POST /auth/logout-all`

## 4. 用户上下文与权限

登录后重点验证：

- `GET /user/me`
- `GET /user/context`
- `GET /user/permission-demo`
- `GET /user/list`

建议对比：

- `admin` 调 `GET /user/list`：应通过
- `manager` 调 `GET /user/list`：应通过
- `user` 调 `GET /user/list`：应被权限拒绝

## 5. MyBatis 演示链

### 5.1 先看总览和原始数据

- `GET /mybatis-demo/accounts`
- `GET /mybatis-demo/overview`
- `GET /mybatis-demo/raw/all`（仅 `admin`）

重点看：

- `rawTotalCount` 是否大于 `tenantVisibleCount`
- `raw/all` 能否看到租户 `1002` 的数据
- 普通账号是否只能看到租户 `1001` 的数据

### 5.2 租户隔离

- `GET /mybatis-demo/tenant/list`
- `GET /mybatis-demo/tenant/page?current=1&size=2`

重点看：

- `admin / manager / auditor / user` 都不应看到 `tenant_id=1002` 的记录
- 分页接口应正常返回分页结构

### 5.3 数据权限

- `GET /mybatis-demo/scope/dept`
- `GET /mybatis-demo/scope/self`
- `GET /mybatis-demo/scope/custom-approved`

建议对比：

- `manager` 调 `/scope/dept`：应看到 `dept_id=20` 和 `21`
- `user` 调 `/scope/self`：应只看到 `create_by=4` 的记录
- `user` 调 `/scope/dept`：应被权限拒绝
- `manager` 调 `/scope/custom-approved`：应只看到 `status=APPROVED`

### 5.4 审计与自动填充

先用 `manager` 或 `user` 调：

- `POST /mybatis-demo/records`

示例请求体：

```json
{
  "title": "新的演示记录",
  "content": "验证自动填充",
  "status": "DRAFT"
}
```

再看返回值中的：

- `tenantId`
- `deptId`
- `createBy`
- `updateBy`
- `createTime`
- `updateTime`

随后调用：

- `GET /mybatis-demo/records/{id}/audit`

核对可见记录和原始表记录是否一致。

补充说明：

- 自动填充最终作用在 `DemoRecordEntity`
- controller 入参 DTO 不会参与自动填充
- demo 实体已经同时配置了 `@AutoFillField` 和 `@TableField(fill = ...)`

### 5.5 乐观锁

先正常更新：

- `PUT /mybatis-demo/records/{id}`

示例请求体：

```json
{
  "version": 1,
  "title": "正常更新",
  "content": "应成功",
  "status": "APPROVED"
}
```

再模拟冲突：

- `POST /mybatis-demo/records/{id}/optimistic-lock-conflict`

重点看：

- `firstUpdateRows` 应为 `1`
- `secondUpdateRows` 应为 `0`
- `optimisticLockWorked` 应为 `true`

### 5.6 细粒度权限点 + SELF_ONLY

使用 `user / 123456`：

- `PUT /mybatis-demo/records/3/self`：应成功
- `PUT /mybatis-demo/records/2/self`：应失败，因为不是自己创建的记录

## 6. 文件模块

- `POST /files/upload/manual`
- `GET /files/download`
- `DELETE /files/delete`
- `POST /files/chunk/init`
- `POST /files/chunk/part`
- `GET /files/chunk/status`
- `POST /files/chunk/complete`
- `DELETE /files/chunk/abort`

## 7. 上下文透传

- `GET /context-trans/async/user`
- `GET /context-trans/async/data-scope`
- `POST /context-trans/async/upload`

重点看：

- `sync` 和 `async` 里的 `principalId / principalName / tenantId / deptId` 是否一致
- `GET /context-trans/async/data-scope` 的 `async.dataScopeType` 是否为 `DEPT_AND_CHILD`
- `POST /context-trans/async/upload` 的 `uploadContextEffective` 是否和 `polo.context-trans.propagate-upload-context && polo.storage.enabled` 一致
- 上传接口里的 `async.uploadContext.filepath` 是否和 `currentUpload.filepath` 一致

## 8. 内容安全与参数校验

- `GET /validate/phone`
- `GET /validate/email`
- `POST /validate/content`
- `POST /validate/full`
- `GET /validate/dynamic-words`
- `POST /validate/dynamic-words`
- `DELETE /validate/dynamic-words`
- `POST /validate/dynamic-words/refresh`

动态词库建议这样测：

1. 使用 `admin / 123456` 登录
2. 调 `POST /validate/dynamic-words` 新增一条词
3. 再调 `POST /validate/content` 提交包含该词的文本，确认命中
4. 调 `DELETE /validate/dynamic-words` 删除该词
5. 再调一次 `POST /validate/content`，确认该词不再命中

## 9. 限流与防重复提交

- `GET /rate-limit/global`
- `GET /rate-limit/user`
- `POST /rate-limit/submit`
- `POST /rate-limit/submit-immediate`

## 10. 异常处理

- `GET /exception/biz`
- `GET /exception/system`
- `POST /exception/validation`
- `GET /exception/missing-param`
