# damai-server（业务微服务集群）

大麦网仿站项目的**业务微服务聚合父模块**（`packaging=pom`，`artifactId: damai-server`），聚合了 9 个子模块。父模块本身不含代码，仅负责统一聚合构建；所有业务服务均注册到 Nacos，经网关对外暴露，服务间通过 OpenFeign（客户端包见 `damai-server-client`）相互调用。

## 功能说明（子模块分工）

| 子模块 | 定位 | 核心能力 |
| --- | --- | --- |
| `damai-gateway-service` | 网关服务 | Spring Cloud Gateway 统一入口（默认端口 6085）；`RequestValidationFilter` / `ResponseValidationFilter` 做请求校验与响应包装，`TokenService` 校验登录令牌，`ApiRestrictService` 做接口限流/限制，`HystrixFallBackController` 提供降级兜底 |
| `damai-user-service` | 用户服务 | 注册/登录/登出、JWT Token 管理（`TokenService`）、购票人管理（`TicketUserService`）、图形验证码（`UserCaptchaService`） |
| `damai-base-data-service` | 渠道基础数据服务 | 渠道信息与 API 限制规则数据（`ChannelDataService`）、省市区域数据（`AreaController`），供网关做请求鉴权与限流的数据支撑 |
| `damai-customize-service` | 定制服务 | API 限制规则的定制管理（`RuleController` / `DepthRuleController` / `AllRuleController`）、API 调用记录（`ApiDataController`）、通过 Kafka `ApiDataMessageConsumer` 消费调用记录、`ServiceBroadcastCall` 向各服务广播规则变更 |
| `damai-program-service` | 演出节目服务 | 节目/场次/票档/座位管理（`ProgramService`、`SeatService`、`TicketCategoryService`），节目数据初始化与多级缓存（布隆过滤器、Redis Stream、延迟队列同步 DB）、ES 搜索（`es` 包）、定时任务 |
| `damai-order-service` | 订单服务 | 订单创建（含 Kafka 异步下单）、支付、取消（超时未支付经延迟队列自动关单）、查询；支付/取消时通过 Lua 脚本回退/确认节目余票与座位缓存 |
| `damai-pay-service` | 支付服务 | 支付/退款统一入口（`PayController`），策略模式封装支付渠道（`PayStrategyContext` + `alipay` 支付宝实现），支付/退款对账（`PayBillService` / `RefundBillService`） |
| `damai-admin-service` | 监控服务 | Spring Boot Admin Server（`@EnableAdminServer`）监控各微服务实例，异常时通过 `DingTalkMessage` 推送钉钉告警 |
| `damai-mybatis-plus-service` | 代码生成工具 | 非运行时服务，仅含 `MybatisPlusGenerator`，基于 MyBatis-Plus Generator + Freemarker 生成各服务的 entity/mapper/service 代码 |

## 调用关系

```
前端(vue3) → gateway → user / program / order / customize
                    order → pay（下单支付）、program（节目数据/限购校验）
                 program → base-data（渠道数据）、order（下单校验）
                    user → base-data
    customize ↔ 各服务（规则变更广播、API 调用记录采集）
                    admin ← 监控所有已注册服务
```

服务间调用均通过 `damai-server-client` 下的 Feign 客户端包（`damai-user-client`、`damai-base-data-client`、`damai-program-client`、`damai-order-client`、`damai-pay-client` 等）完成。

## 技术栈

各子服务统一采用：Spring Boot 3 + Spring Cloud 2023（Gateway / OpenFeign / LoadBalancer）+ Spring Cloud Alibaba Nacos（注册发现）、MySQL + MyBatis-Plus + ShardingSphere 分库分表、Redis + Redisson（分布式锁、延迟队列）、Kafka（下单消息、调用记录）、Elasticsearch（节目搜索）、JWT 登录态、Knife4j 接口文档、Jasypt 配置加密、Spring Boot Admin 监控。

## 与其他模块的关系

- **依赖的项目内模块**：各服务普遍依赖 `damai-common`（统一返回体 `ApiResponse`、工具类）与 `damai-spring-cloud-framework`（`damai-service-common` / `damai-service-component` / 灰度过渡框架）；并按需引入 `damai-redis-tool-framework`（缓存/Lua/Stream）、`damai-redisson-framework`（分布式锁、延迟队列）、`damai-elasticsearch-framework`、`damai-id-generator-framework`、`damai-thread-pool-framework`、`damai-captcha-manage-framework`（验证码）以及 `damai-server-client` 下的 Feign 客户端包。
- **被依赖情况**：本父模块及各服务模块（除 `damai-mybatis-plus-service` 仅作开发期代码生成工具外）均为可独立部署的运行模块，不被其他模块以 Maven 依赖方式引用；服务间的调用契约由 `damai-server-client` 承担。
