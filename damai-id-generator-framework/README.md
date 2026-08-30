# id生成器（damai-id-generator-framework）

## 介绍

- 基于开源百度分布式id源码级别改造来适配spring-boot项目结构，使用时可直接引入该模块即可
- 在原有数据库的基础上新增了redis的方式，如果配置了spring-boot的redis参数配置，即可自动为redis生成workId

## 功能说明

本模块是项目的**分布式 ID 生成器**，以 jar 形式被各业务服务直接依赖，通过 Spring Boot 自动装配（`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`）开箱即用。包含两套 ID 生成方案：

- **雪花算法（Snowflake）**：`SnowflakeIdGenerator` 生成 64 位长整型 ID（时间戳 + 数据中心 ID + 机器 ID + 序列号），支持时钟回拨处理；`getOrderNumber(userId, tableCount)` 额外支持在 ID 末尾融入 `userId % tableCount` 的基因值，用于分库分表路由。
  - `WorkAndDataCenterIdHandler`：通过 Lua 脚本（`resources/lua/workAndDataCenterId.lua`）在 Redis 中原子分配 `workId` 和 `dataCenterId`，避免多实例重复。
  - `IdGeneratorAutoConfig`：自动装配 `SnowflakeIdGenerator` 等 Bean。
- **百度 UidGenerator 改造版**（`com.baidu.fsg.uid` 包下源码级改造）：
  - `DefaultUidGenerator`：默认实现，每次从数据库步进取号。
  - `CachedUidGenerator`：基于 `RingBuffer` 环形缓冲区的缓存实现，异步填充号段，高并发场景性能更好。
  - `WorkerIdAssigner` 两种分配方式：`DisposableWorkerIdAssigner`（数据库 `WORKER_NODE` 表，建表脚本见 `resources/scripts/WORKER_NODE.sql`）和 `RedisDisposableWorkerIdAssigner`（Redis 自增 `uid_work_id`，由 `IdGeneratorRedisConfig` 在配置了 `spring.data.redis.host` 时自动启用）。

## 技术栈

- Spring Boot 3（`spring-boot-starter-web`，自动装配机制）
- Spring Data Redis（`spring-boot-starter-data-redis`，workId 分配 + Lua 脚本）
- Apache Commons Collections / Commons Lang
- MyBatis + MySQL（百度 UidGenerator 的 worker_node 表方案，配置见 `resources/uid/`）

## 与其他模块的关系

- **依赖的项目内模块**：`damai-common`（使用其中的 `BaseCode`、`DaMaiFrameException` 等通用枚举与异常）。
- **被以下业务服务依赖**（`damai-server` 下）：
  - `damai-gateway-service`
  - `damai-user-service`
  - `damai-base-data-service`
  - `damai-customize-service`
  - `damai-program-service`
  - `damai-order-service`
  - `damai-pay-service`

各服务引入本模块后即可注入 `SnowflakeIdGenerator` 生成全局唯一 ID（如订单号、节目 ID 等），其中订单服务利用 `getOrderNumber` 的用户基因能力配合 ShardingSphere 分库分表路由。
