# damai-redisson-framework

基于 Redisson 的分布式基础能力封装（父模块，packaging=pom），为各业务微服务提供分布式锁、防重复执行、布隆过滤器、延迟队列等 Redis 之上的高并发工具。

## 功能说明

本模块不直接包含业务代码，按职责拆分为两层子模块：

- `damai-redisson-service-framework`：分布式服务工具集，本身也是父模块，包含 4 个子模块：
  - `damai-redisson-common-framework`：Redisson 公共配置与基础组件（`RedissonClient` 自动装配、本地锁缓存、锁 key 解析工厂）。
  - `damai-service-lock-framework`：注解式分布式锁（`@ServiceLock`）。
  - `damai-repeat-execute-limit-framework`：注解式防重复执行/幂等控制（`@RepeatExecuteLimit`）。
  - `damai-bloom-filter-framework`：基于 Redisson 的布隆过滤器（`BloomFilterHandler`）。
- `damai-service-delay-queue-framework`：基于 Redisson `RDelayedQueue` 的延迟队列（`DelayProduceQueue` / `DelayConsumerQueue`），用于订单超时关单等延迟任务。

## 技术栈

- Spring Boot 3（自动装配 `AutoConfiguration`）
- Redisson（`redisson-spring-boot-starter`）
- Spring Data Redis、Spring AOP、Caffeine

## 与其他模块的关系

- 依赖：`damai-common`（通用工具/异常/返回体，经由 `damai-redisson-common-framework` 传递）。
- 被依赖：业务微服务按需引入其子模块，如
  - `damai-user-service`、`damai-program-service` 使用分布式锁 + 布隆过滤器；
  - `damai-customize-service`、`damai-program-service`、`damai-order-service` 使用防重复执行；
  - `damai-program-service`、`damai-order-service` 使用延迟队列；
  - `damai-gateway-service`、`damai-base-data-service`、`damai-pay-service` 使用分布式锁。
