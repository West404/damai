# damai-redis-tool-framework

Redis 工具组件聚合模块（`packaging=pom`），为大麦网仿站项目提供统一的 Redis 访问能力封装，包括基础 `RedisTemplate` 配置、缓存操作 API 以及 Redis Stream 消息队列支持。

## 子模块分工

| 子模块 | 说明 |
| --- | --- |
| `damai-redis-common-framework` | Redis 基础配置，提供统一序列化的 `redisToolRedisTemplate` / `redisToolStringRedisTemplate` Bean，通过 Spring Boot 自动装配生效 |
| `damai-redis-framework` | Redis 缓存操作核心封装，提供 `RedisCache` 接口（String/Hash/List/Set/ZSet 全类型操作）及统一的 Key 管理（`RedisKeyBuild` + `RedisKeyManage`） |
| `damai-redis-stream-framework` | Redis Stream 消息队列封装，提供消息推送（`RedisStreamPushHandler`）、消费者监听容器（`StreamMessageListenerContainer`）及消费组/广播两种消费模式 |

子模块间依赖关系：`damai-redis-framework` 与 `damai-redis-stream-framework` 均依赖 `damai-redis-common-framework`。

## 技术栈

- Spring Boot 3.x（`spring-boot-starter-web`、`spring-boot-starter-data-redis`）
- Spring Data Redis（Lettuce）
- Fastjson（对象与 JSON 互转）
- Lombok

## 与其他模块的关系

- 依赖项目内模块：`damai-common`（通用工具类、异常、枚举等）
- 被依赖方：
  - `damai-redis-framework` 被 `damai-server` 下的 `damai-user-service`、`damai-gateway-service`、`damai-program-service`、`damai-base-data-service`、`damai-customize-service`、`damai-order-service`、`damai-pay-service` 等微服务引入，用于业务缓存读写
  - `damai-redis-stream-framework` 被 `damai-program-service` 引入，用于节目相关数据的 Redis Stream 消息同步

## 使用方式

各微服务在 `pom.xml` 中引入对应子模块即可，自动装配通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册，无需额外扫描配置。
