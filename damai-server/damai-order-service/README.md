# damai-order-service（订单服务）

大麦网仿站项目中的**订单微服务**，负责订单的创建、支付、取消、查询及订单状态流转，是整个交易链路的核心服务。启动类为 `com.damai.OrderApplication`（`@EnableDiscoveryClient` + `@EnableFeignClients`），默认通过 Nacos 注册发现。

## 功能说明

- **订单创建**：`OrderController` 提供 `/order/create` 接口（仅允许 program 服务内部调用）；同时通过 Kafka 消费者 `CreateOrderConsumer` 消费下单消息异步创建订单，并对延迟超时的消息执行缓存回滚。创建时生成订单主表 `Order` 与购票人明细 `OrderTicketUser`，并维护「账户-节目」订单数量的 Redis 计数。
- **订单支付**：`/order/pay` 调用 pay 服务发起支付；`/order/alipay/notify` 接收支付宝异步回调；`/order/pay/check` 提供支付后的主动对账检查（支付状态不一致时触发退款）。
- **订单取消**：用户主动取消走 `initiateCancel`（仅限未支付订单）；超时未支付由延迟队列消费者 `DelayOrderCancelConsumer` 自动关单。
- **订单查询**：订单列表 `/order/select/list`、订单详情 `/order/get`（聚合用户与购票人信息）、节目下单数量统计 `accountOrderCount` 等。
- **订单状态联动**：`updateOrderRelatedData` 在订单支付/取消时，通过 `OrderProgramCacheResolutionOperate` 执行 Redis Lua 脚本回退/确认节目余票与座位缓存，支付成功后经 `DelayOperateProgramDataSend` 发送延迟消息同步节目数据到数据库。
- **定时任务**：`OrderDataTask` 定期物理清理历史订单数据。
- 关键并发控制：通过 `@ServiceLock`（分布式锁）与 `@RepeatExecuteLimit`（防重复执行）保证订单状态更新与 MQ 消费的幂等性。

## 技术栈

- Spring Boot 3.x（Web、Validation、Actuator）+ Spring Cloud（OpenFeign + OkHttp、LoadBalancer）+ Spring Cloud Alibaba Nacos（注册发现）
- MySQL + MyBatis-Plus（持久层，配合 ShardingSphere 分库分表，由公共配置引入）
- Redis（`RedisCache` / Lua 脚本操作节目缓存）+ Redisson（分布式锁、延迟队列）
- Kafka（下单消息异步消费）
- 百度 `UidGenerator`（分布式 ID，雪花算法）
- Jasypt（配置加密）、Spring Boot Admin Client（服务监控）、Hutool / Fastjson（工具类）

## 与其他模块的关系

**项目内依赖**（见本模块 `pom.xml`）：

- `damai-common`：通用工具、异常、统一返回体 `ApiResponse`
- `damai-order-client`：本服务的 Feign 客户端包（DTO/VO 定义）
- `damai-pay-client`、`damai-program-client`：调用支付服务、节目服务的 Feign 接口
- `damai-service-common`、`damai-service-component`（damai-spring-cloud-framework）：微服务公共组件
- `damai-redis-framework`（damai-redis-tool-framework）：Redis 缓存与 Lua 脚本支持
- `damai-service-lock-framework`、`damai-repeat-execute-limit-framework`（damai-redisson-framework）：分布式锁与防重复执行注解
- `damai-service-delay-queue-framework`（damai-redisson-framework）：延迟队列（超时关单、节目数据延迟同步）
- `damai-id-generator-framework`：分布式 ID 生成
- `damai-thread-pool-framework`：线程池
- `damai-service-gray-transition-webmvc-framework`：灰度发布过渡支持

**被依赖情况**：本服务为可独立部署的运行模块，不被其他服务以 Maven 依赖方式引用；其对外契约通过 `damai-order-client` 暴露，目前被 `damai-program-service` 依赖（节目服务下单、限购校验时调用本服务）。
