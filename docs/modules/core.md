# polo-boot-core

## 模块作用

`polo-boot-core` 是整个项目的公共底座，主要提供：

- 统一返回体 `Result`
- 错误码和状态语义
- 业务异常 `BizException`
- 模块之间共享的上下文契约

如果你只打算先接一个模块，通常建议先从 `core` 开始。

## 适合谁

- 几乎所有业务项目
- 想统一接口响应格式的项目
- 想统一业务异常表达方式的项目
- 想为后续 `security`、`mybatis-plus` 等模块提供公共契约的项目

## 关键类

- [`Result.java`](../../polo-boot-core/src/main/java/com/polo/boot/core/model/Result.java)
- [`BizException.java`](../../polo-boot-core/src/main/java/com/polo/boot/core/exception/BizException.java)
- [`CurrentPrincipal.java`](../../polo-boot-core/src/main/java/com/polo/boot/core/context/CurrentPrincipal.java)
- [`CurrentPrincipalProvider.java`](../../polo-boot-core/src/main/java/com/polo/boot/core/context/CurrentPrincipalProvider.java)
- [`SecurityContextFacade.java`](../../polo-boot-core/src/main/java/com/polo/boot/core/context/SecurityContextFacade.java)

## 接入方式

```xml
<dependency>
    <groupId>io.github.smiling11123</groupId>
    <artifactId>polo-boot-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

## 常见用法

### 统一返回体

```java
return Result.success(data);
```

```java
return Result.success("操作成功");
```

```java
return Result.fail("操作失败");
```

### 业务异常

```java
throw new BizException("参数错误");
```

如果你的项目引入了 [`polo-boot-web`](./web.md)，这些异常会进一步被全局异常处理器统一封装成标准响应。

## 模块定位

`core` 本身不直接处理请求、认证、数据库和文档，它更像：

- 公共模型层
- 异常语义层
- 上下文契约层

后面的 `web`、`security`、`mybatis-plus` 都会建立在它之上。
