# polo-boot-cache

## 模块作用

`polo-boot-cache` 提供 Redis 能力的统一封装，当前主要是：

- `RedisService`

它适合那些已经在用 Redis，但不想在业务里反复直接操作 `StringRedisTemplate` 或自己手写序列化逻辑的项目。

## 关键类

- [`CacheAutoConfiguration.java`](../../polo-boot-cache/src/main/java/com/polo/boot/cache/config/CacheAutoConfiguration.java)
- [`CacheProperties.java`](../../polo-boot-cache/src/main/java/com/polo/boot/cache/config/CacheProperties.java)
- [`RedisService.java`](../../polo-boot-cache/src/main/java/com/polo/boot/cache/service/RedisService.java)
- [`RedisServiceImpl.java`](../../polo-boot-cache/src/main/java/com/polo/boot/cache/service/impl/RedisServiceImpl.java)

## 接入方式

```xml
<dependency>
    <groupId>io.github.smiling11123</groupId>
    <artifactId>polo-boot-cache</artifactId>
    <version>0.1.1</version>
</dependency>
```

## 配置项

Redis 基础配置仍然使用 Spring Boot 官方配置：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

模块开关：

```yaml
polo:
  cache:
    enabled: true
    redis-service-enabled: true
```

配置说明：

- `enabled`：是否启用 cache 模块自动装配
- `redis-service-enabled`：是否注册 `RedisService`

## 使用示例

```java
redisService.set("user:1", user, 30, TimeUnit.MINUTES);
```

```java
User user = redisService.get("user:1", User.class);
```

```java
User user = redisService.get("user:1", new TypeReference<T>() {});
```

```java
redisService.delete("user:1");
```

```java
Long value = redisService.increment("order:seq", 1L);
```

## 适合的场景

- 缓存用户资料
- 短期验证码缓存
- 计数器
- 幂等控制
- 会话辅助信息

如果你后续接入 [`polo-boot-security`](./security.md)，项目里的登录会话和防重复提交也会依赖 Redis，但它们会各自走自己的逻辑，不要求你业务代码直接操作缓存。
