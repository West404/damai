# damai-pay-service（支付服务）

大麦网仿站项目中的支付微服务，启动类为 `com.damai.PayApplication`，默认端口 `6087`，服务名 `damai-pay-service`。

## 功能说明

提供统一的支付、回调、对账、退款能力，对外通过 Feign（`damai-pay-client`）被其他服务调用，不直接暴露给前端。

核心接口（`PayController`，路径前缀 `/pay`）：

- `POST /pay/common/pay`：通用支付入口，基于订单号加分布式锁（`@ServiceLock`）保证幂等，不依赖第三方支付渠道的幂等性
- `POST /pay/notify`：支付结果异步回调通知，验签 + 数据校验（金额、商户、appId、交易状态）后更新账单
- `POST /pay/trade/check`：主动查询第三方交易状态并与本地账单对账，状态不一致时以渠道为准修正
- `POST /pay/refund`：退款，校验账单状态与退款金额后调用渠道退款并落退款单
- `POST /pay/detail`：账单详情查询

核心类与设计：

- `PayStrategyHandler`：支付策略接口（支付/验签/数据校验/交易查询/退款），`PayStrategyContext` 按支付渠道路由到具体策略
- `AlipayStrategyHandler`：支付宝渠道实现，基于官方 `alipay-sdk-java`（电脑网站支付 FAST_INSTANT_TRADE_PAY）
- `PayStrategyInitHandler`：启动时扫描所有 `PayStrategyHandler` 实现并注册到策略上下文（新增渠道只需实现接口并注册为 Bean）
- `PayAutoConfig`：支付相关自动装配（`AlipayClient`、策略上下文等），通过 `META-INF/spring/...AutoConfiguration.imports` 生效
- 数据层：`PayBill`（支付账单）、`RefundBill`（退款账单）实体 + MyBatis-Plus Mapper，使用 ShardingSphere 分库分表（配置见 `shardingsphere-pay-*.yaml`）

## 技术栈

- Spring Boot 3（Web、Validation、Actuator）+ Spring Cloud（OpenFeign + OkHttp、LoadBalancer）
- Spring Cloud Alibaba Nacos（服务注册发现）
- MySQL + MyBatis-Plus + ShardingSphere（分库分表，`ShardingSphereDriver` 作为数据源）
- 支付宝开放平台 SDK（`alipay-sdk-java`）
- Jasypt（配置加密）、Spring Boot Admin Client（监控）、Log4j2（日志）

## 与其他模块的关系

依赖的项目内模块：

- `damai-common`：通用工具、统一返回体 `ApiResponse`、异常与枚举（`BaseCode`、`PayBillStatus`、`PayChannel` 等）
- `damai-pay-client`：本服务的 Feign 客户端包（DTO/VO 与 `PayClient` 定义），服务端实现其接口契约
- `damai-service-common` / `damai-service-component` / `damai-service-initialize`：微服务通用配置、组件与启动初始化框架（`PayStrategyInitHandler` 继承其 `AbstractApplicationInitializingBeanHandler`）
- `damai-service-lock-framework`：业务分布式锁（`@ServiceLock`，用于支付/对账幂等）
- `damai-redis-framework`：Redis 缓存支持
- `damai-id-generator-framework`：分布式 ID（`UidGenerator`，生成账单/退款单主键）
- `damai-service-gray-transition-webmvc-framework`：灰度发布过渡支持

被依赖情况：

- 没有任何模块直接依赖本服务 jar；`damai-order-service` 通过 `damai-pay-client` 以 Feign 方式远程调用本服务的支付、回调、退款等接口。
