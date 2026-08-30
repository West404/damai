# damai-bloom-filter-framework

基于 Redisson `RBloomFilter` 的布隆过滤器工具，用于缓存层防穿透。

## 功能说明

- `BloomFilterHandler`：布隆过滤器操作封装，初始化时按配置 `tryInit` 容量与误判率，提供 `add` / `contains` 以及容量、误判率、元素数等查询方法；过滤器名自动拼接应用区分前缀（`SpringUtil.getPrefixDistinctionName()`）。
- `BloomFilterProperties`：配置项，包含过滤器名（`name`）、预期插入量（`expectedInsertions`）、误判率（`falseProbability`）。
- `BloomFilterAutoConfiguration`：自动装配，注册 `BloomFilterHandler` Bean。

## 技术栈

- Spring Boot 3（自动装配）
- Redisson（`RBloomFilter`，经由 `damai-redisson-common-framework`）

## 与其他模块的关系

- 依赖：`damai-redisson-common-framework`（`RedissonClient`）。
- 被依赖：业务微服务 `damai-user-service`、`damai-program-service`（如用户、节目数据查询前的缓存穿透防护）。
