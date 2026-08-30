# damai-service-lock-framework

注解式分布式锁工具，基于 Redisson 实现，支持声明式与编程式两种用法。

## 功能说明

- `@ServiceLock` 注解：标注在方法或类上即可加锁，可配置锁类型（`lockType`）、业务名（`name`）、SpEL 锁 key（`keys`）、等待时间（`waitTime`）及加锁超时处理策略（`lockTimeoutStrategy`，默认 FAIL 快速失败，也支持自定义策略 Bean）。
- `ServiceLockAspect`：AOP 切面，拦截 `@ServiceLock` 方法完成加锁/解锁。
- 锁类型与实现：`LockType` 支持可重入锁、公平锁、读锁、写锁，分别由 `RedissonReentrantLocker`、`RedissonFairLocker`、`RedissonReadLocker`、`RedissonWriteLocker` 实现，`ServiceLockFactory` 负责按类型选择。
- `ServiceLocker` / `ManageLocker`：锁的统一抽象与管理。
- `ServiceLockTool`：编程式加锁工具，配合 `TaskRun` / `TaskCall` 在锁内执行业务逻辑。

## 技术栈

- Spring Boot 3（AOP）
- Redisson（经由 `damai-redisson-common-framework`）

## 与其他模块的关系

- 依赖：`damai-redisson-common-framework`（`RedissonClient` 与锁 key 解析组件）。
- 被依赖：`damai-repeat-execute-limit-framework`（幂等控制复用分布式锁）；业务微服务 `damai-gateway-service`、`damai-user-service`、`damai-base-data-service`、`damai-customize-service`、`damai-program-service`、`damai-order-service`、`damai-pay-service`。
