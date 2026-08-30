# damai-redisson-service-framework

Redisson 分布式服务工具集（父模块，packaging=pom），聚合 Redisson 公共配置及其上层的分布式锁、幂等、布隆过滤器能力。

## 功能说明

本模块不含源码，各子模块分工如下：

- `damai-redisson-common-framework`：Redisson 公共配置。`RedissonCommonAutoConfiguration` 在 Redisson 官方自动装配之前创建 `RedissonClient`（单机模式，可配线程池），并注册 `RedissonDataHandle`、`LocalLockCache`（基于 Caffeine 的本地锁）、`LockInfoHandleFactory`（SpEL 锁 key 解析）。
- `damai-service-lock-framework`：注解式分布式锁。提供 `@ServiceLock` 注解 + `ServiceLockAspect` 切面，支持可重入锁/公平锁/读锁/写锁（`LockType`，对应 `RedissonReentrantLocker` 等实现），内置加锁超时策略（`LockTimeOutStrategy`）与编程式工具 `ServiceLockTool`。
- `damai-repeat-execute-limit-framework`：防重复执行/幂等控制。提供 `@RepeatExecuteLimit` 注解 + `RepeatExecuteLimitAspect`，结合本地锁与分布式锁保证接口幂等。
- `damai-bloom-filter-framework`：布隆过滤器。`BloomFilterHandler` 封装 Redisson `RBloomFilter`，`BloomFilterProperties` 配置预期插入量与误判率，用于防缓存穿透。

## 技术栈

- Spring Boot 3（自动装配、AOP）
- Redisson、Spring Data Redis、Caffeine

## 与其他模块的关系

- 依赖：底层子模块依赖 `damai-common`。
- 被依赖：业务微服务按需引入其子模块（分布式锁被 gateway/user/base-data/customize/program/order/pay 使用，布隆过滤器被 user/program 使用，防重复执行被 customize/program/order 使用）。
