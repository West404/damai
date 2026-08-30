# damai-gateway-service 网关服务

大麦网仿站项目的统一入口网关，基于 Spring Cloud Gateway 构建。所有外部请求先经过本网关，在此完成签名校验、加解密、Token 认证、限流防刷、灰度路由等横切逻辑后，再路由转发到下游各业务微服务。默认端口 `6085`，启动类为 `com.damai.GatewayApplication`。

## 功能说明

### 请求/响应过滤（`com.damai.filter`）

- `RequestValidationFilter`：全局请求过滤器（order = -2），核心能力包括：
  - 链路追踪：生成/透传 `traceId`（基于百度 `UidGenerator`），写入 MDC 并传递给下游服务；
  - 灰度标识：透传灰度请求头（配合灰度路由框架）；
  - RSA 验签与解密：根据请求中的渠道编码 `code` 获取渠道密钥（`ChannelDataService`），对请求体做 RSA-SHA256 验签，支持 `V2` 加密请求体的解密；
  - Token 认证：解析并校验用户 Token（`TokenService`），按 `GatewayProperty` 配置的路径白名单控制是否必须登录；
  - 接口防刷：调用 `ApiRestrictService` 对配置的接口做频率限制；
  - 将 `userId`、`code` 等参数写入请求头，传递给下游微服务。
- `ResponseValidationFilter`：全局响应过滤器，对 `V2` 加密请求对应的响应数据做 RSA 加密后再返回给客户端。

### 接口限流与防刷

- `ApiRestrictService` + `ApiRestrictCacheOperate`：读取 Redis 中配置的普通规则（`RuleVo`）和深度时间窗口规则（`DepthRuleVo`），通过 Lua 脚本（`lua/apiLimit.lua`）在 Redis 中原子化统计调用次数，触发阈值时拒绝请求；
- 触发限制的请求记录（`ApiDataDto`）通过 Kafka（`ApiDataMessageSend`，topic 默认 `save_api_data`）异步落库；
- `pro.limit.RateLimiter`：基于 `Semaphore` 的本地令牌限流，可通过开关 `RateLimiterProperty` 启用。

### 熔断与限流降级

- `GatewaySentinelConfiguration`：集成 Sentinel 网关适配器（`SentinelGatewayFilter`），规则可通过 Nacos 数据源下发；
- `HystrixFallBackController`：提供 `/fallBackHandler` 熔断降级兜底接口；
- `GatewayDefaultExceptionHandler`：网关统一异常处理。

### 通用配置（`com.damai.conf`）

- `Config`：跨域（CORS）配置、`ThreadPoolExecutor` 线程池（用于异步调用下游接口）、`RestTemplate` 等；
- Knife4j 网关聚合：以服务发现模式聚合下游各微服务的 OpenAPI 文档。

## 技术栈

- Spring Cloud Gateway（WebFlux 响应式网关）+ Spring Cloud LoadBalancer
- Spring Cloud Alibaba Nacos（服务注册发现）
- Spring Cloud OpenFeign（feign-okhttp，调用下游服务）
- Sentinel（sentinel-spring-cloud-gateway-adapter + sentinel-datasource-nacos，限流熔断）
- Spring Kafka（接口调用记录异步落库）
- Knife4j Gateway（API 文档聚合）
- JWT + RSA 签名/加密（`damai-common` 中的 `TokenUtil`、`RsaTool` 等工具）
- Redis Lua 脚本（防刷规则统计）
- Jasypt（配置加密）、Actuator + Prometheus（监控）

## 与其他模块的关系

本模块是独立的可执行服务（叶子模块），**没有其他模块依赖它**，仅被父模块 `damai-server` 聚合。

### 依赖的项目内模块

| 模块 | 用途 |
| --- | --- |
| `damai-common` | 通用返回体 `ApiResponse`、异常体系、枚举、工具类（`RsaTool`、`TokenUtil` 等） |
| `damai-id-generator-framework` | 百度 `UidGenerator` 分布式 ID，用于生成 `traceId` 和接口记录 ID |
| `damai-redis-framework` | Redis 缓存封装 `RedisCache`、Lua 脚本执行、限流规则读写 |
| `damai-base-data-client` | 基础数据服务的 Feign 客户端，`ChannelDataService` 通过它查询渠道密钥数据 |
| `damai-service-gray-transition-gateway-framework` | 灰度发布网关路由框架，按请求头中的灰度标识路由到灰度/生产实例 |
| `damai-service-lock-framework` | 分布式锁能力 |

### 运行时协作的下游服务

请求经网关校验后，由 Nacos 服务发现 + LoadBalancer 路由到 `damai-server` 下的各业务微服务（user、base-data、customize、program、order、pay、admin 等）；同时通过 `damai-base-data-client` 以 Feign 方式调用 `damai-base-data-service` 获取渠道数据（本地 Redis 缓存优先）。
