# damai-program-service（演出节目服务）

## 功能说明

大麦网仿站中的**节目（演出）核心服务**，artifactId 为 `damai-program-service`，启动类 `ProgramApplication`（服务端口 6086）。负责节目、节目类型、演出时间、票档、座位等核心数据的维护，以及高并发场景下的节目缓存、节目搜索和购票下单（余票扣减）能力。

### 核心接口（controller）

- `ProgramController`：节目的添加、搜索、首页列表、推荐列表、详情、分页、下架等
- `ProgramCategoryController`：节目类型（分类）的维护与多级查询
- `ProgramShowTimeController`：节目演出时间管理
- `TicketCategoryController`：票档（票价档位）的添加、详情、列表
- `SeatController`：座位的单个/批量添加与关联信息查询
- `ProgramOrderController`：购票下单，按版本路由到不同实现（V1～V4）
- `ProgramResetController` / `TestController`：数据重置与测试辅助接口

### 关键能力（service）

- **多版本购票策略**：`service.strategy` 包下的策略模式实现，`ProgramOrderContext` 按版本分发到 `ProgramOrderV1Strategy` ～ `ProgramOrderV4Strategy`，逐版本演进（同步下单 → 缓存扣减 → Lua 原子扣减 → 异步下单），对应高并发优化教学路线
- **责任链校验**：`service.composite` 包下的 `AbstractProgramCheckHandler` 及其子类（布隆过滤器校验、节目详情校验、用户存在校验、下单参数校验、推荐校验）
- **多级缓存**：`service.cache.local` 本地缓存 + Redis 缓存 + 布隆过滤器（`ProgramBloomFilterInit`），通过 `service.lua` 包中的 Lua 脚本（`resources/lua/*.lua`）保证缓存操作原子性
- **节目搜索**：基于 Elasticsearch（`service.es.ProgramEs`、`ProgramElasticsearchInitData`），支持节目数据同步 ES 与关键词搜索
- **异步与消息**：Kafka 发送创建订单消息（`service.kafka.CreateOrderSend`）；Redis Stream 消费（`ProgramRedisStreamConsumer`）；延迟队列处理节目数据操作与订单取消（`DelayOperateProgramDataConsumer`、`DelayOrderCancelSend`）
- **定时与初始化**：`ProgramDataTask` 定时任务、`ProgramShowTimeRenewal` 演出时间续约、`ProgramCategoryInitData` 类型初始化、`TokenExpireManager` 令牌过期管理
- **分库分表**：数据源使用 ShardingSphere-JDBC 驱动（`shardingsphere-program-{local|docker|pro}.yaml`），节目/座位等大表按规则分片

## 技术栈

- Spring Boot 3.x Web / Validation / Actuator + Spring Boot Admin Client
- Spring Cloud：Nacos 服务注册发现、OpenFeign（feign-okhttp）、LoadBalancer
- Spring Cloud Alibaba：Sentinel 限流熔断（规则存储于 Nacos，`sentinel-datasource-nacos`）
- MySQL + MyBatis-Plus + ShardingSphere-JDBC 分库分表
- Redis（含 Lua 脚本）+ Redis Stream + Redisson 分布式锁/延迟队列/布隆过滤器
- Elasticsearch（spring-data-elasticsearch）节目搜索
- Kafka 消息队列
- Jasypt 配置加密、Knife4j/Swagger 接口文档、Log4j2 日志
- 项目内线程池、分布式 ID 等自研框架

## 与其他模块的关系

### 本模块依赖的项目内模块

- `damai-common`：通用工具、异常、统一返回体
- `damai-service-common` / `damai-service-component` / `damai-service-initialize`：微服务通用组件与初始化（damai-spring-cloud-framework）
- `damai-redis-framework` / `damai-redis-stream-framework`：Redis 缓存/Lua 与 Stream 消息（damai-redis-tool-framework）
- `damai-service-lock-framework` / `damai-bloom-filter-framework` / `damai-repeat-execute-limit-framework` / `damai-service-delay-queue-framework`：分布式锁、布隆过滤器、防重复执行、延迟队列（damai-redisson-framework 等）
- `damai-id-generator-framework`：分布式 ID 生成
- `damai-thread-pool-framework`：动态线程池
- `damai-elasticsearch-framework`：ES 搜索封装
- `damai-service-gray-transition-webmvc-framework`：灰度发布过渡
- `damai-program-client`：本服务对外暴露的 DTO/VO/枚举/Feign 定义
- `damai-base-data-client` / `damai-order-client`：通过 Feign 调用基础数据服务与订单服务

### 依赖本模块的模块

- 服务本身（可独立部署的 Spring Boot 应用）**不被其他模块以 Maven 依赖方式引用**；其他服务通过 Feign 调用本服务时依赖的是 `damai-server-client/damai-program-client`（如 `damai-order-service` 就依赖 `damai-program-client` 来查询节目信息）。

## 构建与启动

```bash
# 在项目根目录下构建本模块
mvn -pl damai-server/damai-program-service -am clean package

# 运行（需先启动 Nacos、MySQL、Redis、Kafka、ES 等中间件）
java -jar damai-server/damai-program-service/target/damai-program-service-*.jar
```

默认端口 `6086`，分库分表规则见 `src/main/resources/shardingsphere-program-*.yaml`。
