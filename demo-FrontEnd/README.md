# demo-FrontEnd

`demo-FrontEnd` 是给 `polo-boot-starter` demo 配套的 Vue 3 前端示例，目前内置了文件上传工作台和二维码登录测试页。

## 当前页面能力

- 账号密码登录获取 Bearer Token
- 对接 `UploadController` 进行分片上传
- 暂停上传、继续上传、取消上传
- 对接 `/files/download` 进行 Range 下载
- 暂停下载、继续下载、取消下载
- 上传完成后自动把 `filepath / storageType` 回填到下载区域
- 对接 `/auth/qrcode/*` 进行二维码登录测试
- 模拟 PC 端生成二维码、轮询状态
- 模拟移动端登录、扫码、确认登录

## 启动方式

先确保后端 demo 已运行在 `http://localhost:8080`，再在当前目录执行：

```sh
npm install
npm run dev
```

默认访问地址通常是：

- `http://localhost:5173/`

页面入口：

- `http://localhost:5173/upload`
- `http://localhost:5173/qrcode-login`

## 代理说明

Vite 开发代理已经预置：

- `/auth` -> `http://localhost:8080`
- `/files` -> `http://localhost:8080`

所以前端开发时不需要额外处理这两个接口的跨域问题。

## 使用提示

- 分片上传接口要求 `admin` 角色，页面支持一键用 `admin / 123456` 登录获取 Token
- 可恢复下载依赖浏览器 File System Access API，推荐使用 Chrome 或 Edge
- 下载恢复只在当前页面会话内保留文件句柄；刷新页面后如果要继续下载，需要重新开始下载
