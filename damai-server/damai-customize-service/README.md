# damai-customize-service（定制服务）

大麦网仿站项目中的**定制服务**，属于 `damai-server` 业务微服务集群之一，独立部署运行（默认端口 `6084`，启动类 `com.damai.CustomizeApplication`），通过 Nacos 注册发现。

## 功能说明

本模块是平台的**运营配置与接口管控中心**，主要负责：

- **限流规则管理**：维护"普通规则"（`Rule`，按统计时间+阈值限流）和"深度规则"（`DepthRule`，在普通规则基础上增加时间窗口维度），提供新增、修改、启停、查询接口（`RuleController` / `DepthRuleController`），以及整体规则一次性配置的入口（`AllRuleController` / `AllRuleService`）。
- **规则缓存同步**：规则变更后由 `RuleService#saveAllRuleCache()` 将生效中的规则写入 Redis（`RedisKeyManage.ALL_RULE_HASH`），供网关等服务实时读取执行限流。
- **API 调用记录**：`ApiDataMessageConsumer` 消费 Kafka 主题 `save_api_data`（由网关上报接口调用数据），将调用记录（`ApiData`）落库保存；`ApiDataController` 提供分页查询，用于接口调用情况统计分析。消费端通过 `@RepeatExecuteLimit` 注解保证幂等，防止重复消费。
- **广播调用**：`BroadcastController` / `BroadcastService` / `ServiceBroadcastCall` 提供广播调用的预留能力（当前为占位实现）。

核心表：`d_rule`（普通规则）、`d_depth_rule`（深度规则）、`d_api_data`（API 调用记录）。

## 技术栈

- **Spring Boot 3 + Spring Cloud**：Web、Validation、OpenFeign（OkHttp 客户端）、LoadBalancer
- **Spring Cloud Alibaba**：Nacos 服务注册发现、Sentinel 限流熔断（规则数据源接 Nacos）
- **MySQL + MyBatis-Plus**：规则与调用记录的持久化，Mapper 扫描 `com.damai.mapper`
- **Redis**：规则缓存（`damai-redis-framework` 的 `RedisCache`）
- **Kafka**：消费 `save_api_data` 主题，异步保存 API 调用记录
- **分布式 ID**：`damai-id-generator-framework`（百度 UidGenerator）
- **监控与文档**：Actuator + Spring Boot Admin Client、Knife4j/SpringDoc
- **其他**：Jasypt 配置加密、Lombok、Hutool、Fastjson

## 与其他模块的关系

**依赖的项目内模块：**

- `damai-common`：通用工具、异常、统一返回体 `ApiResponse`
- `damai-customize-client`：本服务的 DTO/VO 契约包（`RuleDto`、`ApiDataVo` 等）
- `damai-service-common`、`damai-service-component`：微服务通用组件（常量、初始化等）
- `damai-redis-framework`：Redis 缓存操作与 `RedisKeyManage` 缓存键管理
- `damai-id-generator-framework`：分布式 ID 生成
- `damai-service-lock-framework`：分布式锁
- `damai-repeat-execute-limit-framework`：防重复执行注解（用于 Kafka 消费幂等）
- `damai-service-gray-transition-webmvc-framework`：灰度过渡（WebMVC）

**被依赖 / 交互方：**

- 项目内没有其他模块通过 Maven 依赖本服务或其 client 包，本服务属于相对独立的运营支撑服务，接口主要供后台管理端（经网关路由）调用。
- **网关服务（damai-gateway-service）** 是其主要数据交互方：网关将接口调用记录发送到 Kafka 主题 `save_api_data`，由本服务消费落库；同时网关执行限流时读取本服务写入 Redis 的规则缓存。
