# damai-redisson-common-framework

Redisson 公共配置模块，为分布式锁、防重复执行、布隆过滤器等上层工具提供统一的 `RedissonClient` 与基础组件。

## 功能说明

- `RedissonCommonAutoConfiguration`：核心自动装配类，`@AutoConfigureBefore` Redisson 官方装配，基于 `RedisProperties` 手动创建单机模式 `RedissonClient`（支持 SSL、自定义线程数/Netty 线程数、自定义执行线程池），并注册 `RedissonDataHandle`、`LocalLockCache`、`LockInfoHandleFactory`。
- `RedissonDataHandle`：Redisson 数据操作封装。
- `LocalLockCache`：基于 Caffeine 的本地锁缓存，与分布式锁配合做"本地优先"的两级锁定。
- 锁 key 解析：`LockInfoHandle` / `AbstractLockInfoHandle` / `LockInfoHandleFactory` 配合 `LocalVariableTableParameterNameDiscoverer` 解析 `@ServiceLock`、`@RepeatExecuteLimit` 注解中 SpEL 形式的 key（`LockInfoType` 定义锁类型枚举）。

## 技术栈

- Spring Boot 3（自动装配、AOP）
- Redisson（`redisson-spring-boot-starter`）
- Spring Data Redis、Caffeine

## 与其他模块的关系

- 依赖：`damai-common`（通用工具/异常）。
- 被依赖：本模块是同族模块 `damai-service-lock-framework`、`damai-repeat-execute-limit-framework`、`damai-bloom-filter-framework` 以及 `damai-service-delay-queue-framework` 的共同基础，间接服务于所有使用这些工具的业务微服务。
