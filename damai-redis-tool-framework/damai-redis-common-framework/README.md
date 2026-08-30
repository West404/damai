# damai-redis-common-framework

Redis 基础配置封装，是 `damai-redis-tool-framework` 下所有 Redis 相关组件的公共基础。

## 功能说明

- 通过自动装配类 `RedisFrameWorkAutoConfig`（`com.damai.config`）注册两个 Redis 操作模板 Bean：
  - `redisToolRedisTemplate`：通用 `RedisTemplate`，默认使用 `StringRedisSerializer` 序列化
  - `redisToolStringRedisTemplate`：`StringRedisTemplate`（`@Primary`），供项目内 Redis 操作统一注入使用
- 自动装配通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册，引入依赖即可生效

## 技术栈

- Spring Boot 3.x（`spring-boot-starter-web`，已排除默认 logging）
- Spring Data Redis（`spring-boot-starter-data-redis`）

## 与其他模块的关系

- 依赖项目内模块：`damai-common`
- 被依赖方：同父模块下的 `damai-redis-framework`（Redis 缓存操作封装）与 `damai-redis-stream-framework`（Redis Stream 封装），二者注入本模块提供的 `redisToolStringRedisTemplate` Bean
