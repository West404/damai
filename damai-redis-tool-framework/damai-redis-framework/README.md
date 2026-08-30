# damai-redis-framework

Redis 缓存操作核心封装模块，为业务微服务提供统一、类型安全的 Redis 访问 API 与缓存 Key 管理规范。

## 功能说明

- `RedisCache`（接口）/ `RedisCacheImpl`（实现）：覆盖 Redis 五种数据结构的常用操作
  - String：`get` / `set` / `setIfAbsent` / `multiSet` / `incrBy` / `append` 等，支持"查缓存为空则回源加载并回填"（`get(key, clazz, supplier, ttl, unit)`）
  - Hash：`putHash` / `getForHash` / `multiGetForHash` / `getAllMapForHash` / `incrByForHash` 等
  - List：`leftPush` / `rightPop` / 阻塞式 `pop` / `rangeForList` / `trimForList` 等
  - Set / ZSet：交并差集、随机成员、范围查询、分值增减等
  - 通用 Key 操作：`del` / `expire` / `hasKey` / `keys` / `rename` / `type` 等
  - 对象以 Fastjson 序列化为 JSON 字符串存储，读取时自动反序列化为目标类型
- `RedisKeyBuild`：缓存 Key 包装类，基于枚举模板 + 占位符参数构建真实 Key，并自动拼接环境前缀（`SpringUtil.getPrefixDistinctionName()`）
- `RedisKeyManage`：全局缓存 Key 枚举（如 `USER_LOGIN`、`PRODUCT_STOCK`、`PROGRAM`、座位/余票/订单等 Key 模板），集中管理 Key 的格式与含义
- `CacheUtil`：Key 校验、批量 Key 提取、结果集清洗等工具方法
- 自动装配：`RedisCacheAutoConfig` 注册 `RedisCacheImpl` Bean（注入 `redisToolStringRedisTemplate`），通过 `AutoConfiguration.imports` 自动生效

## 技术栈

- Spring Boot 3.x（`spring-boot-starter-web`、`spring-boot-starter-data-redis`）
- Fastjson、Lombok

## 与其他模块的关系

- 依赖项目内模块：`damai-common`、`damai-redis-common-framework`
- 被依赖方：`damai-server` 下的 `damai-user-service`、`damai-gateway-service`、`damai-program-service`、`damai-base-data-service`、`damai-customize-service`、`damai-order-service`、`damai-pay-service` 等业务微服务，用于登录态、节目、座位、库存、订单等业务数据的缓存读写
