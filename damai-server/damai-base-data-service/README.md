# damai-base-data-service（渠道基础数据服务）

大麦网仿站项目中的**基础数据微服务**，隶属于 `damai-server` 业务微服务集群，负责提供全局通用的基础数据查询能力，主要包括**省市地区数据**和**渠道数据**两类。

启动类：`com.damai.BaseDataApplication`（`@EnableDiscoveryClient` + `@EnableFeignClients`，注册到 Nacos 并开启 Feign 调用）。

## 功能说明

- **地区数据**：`AreaController` / `AreaService` / `AreaMapper` / `Area`
  - 查询省市及直辖市数据（优先走 Redis 缓存，未命中回源 MySQL 并回填缓存）
  - 按 id 或 id 集合查询、当前城市、热门城市等接口
- **渠道数据**：`ChannelDataController` / `ChannelDataService` / `ChannelDataMapper` / `ChannelTableData`
  - 通过渠道 code 查询渠道数据
  - 新增渠道数据：使用百度 `UidGenerator` 生成分布式 ID 落库，并同步写入 Redis 缓存
- **测试接口**：`TestController` / `TestService`，用于本地调试验证

接口统一返回 `ApiResponse` 包装体，入参使用 `damai-base-data-client` 中定义的 DTO/VO，配合 `jakarta.validation` 校验，并通过 Knife4j（Swagger 注解）暴露接口文档。

## 技术栈

- Spring Boot 3 / Spring Cloud / Spring Cloud Alibaba（Nacos 服务发现、Sentinel 限流熔断，Sentinel 规则数据源接 Nacos）
- Spring Cloud OpenFeign（feign-okhttp）+ LoadBalancer 服务间调用
- MySQL + MyBatis-Plus 持久层
- Redis 缓存（`damai-redis-framework` 封装的 `RedisCache` / `RedisKeyBuild`）
- 分布式 ID（百度 UidGenerator，来自 `damai-id-generator-framework`）
- 监控可观测：Spring Boot Actuator、Spring Boot Admin Client、Micrometer + Prometheus（含 JVM extras）
- 配置加密 jasypt、灰度过渡框架、`transmittable-thread-local` 上下文透传
- 日志采用 Log4j2（`log4j2.xml`，已排除 spring-boot-starter-logging）

## 与其他模块的关系

**本模块依赖的项目内模块**（见 `pom.xml`）：

| 依赖模块 | 用途 |
| --- | --- |
| `damai-common` | 通用工具类、枚举（`Status`、`AreaType` 等）、常量 |
| `damai-service-common` | 微服务通用基础（`ApiResponse` 返回体等） |
| `damai-service-component` | 微服务通用组件 |
| `damai-base-data-client` | 本服务的 Feign 客户端包，提供 DTO/VO 定义 |
| `damai-redis-framework` | Redis 缓存操作封装（`RedisCache`） |
| `damai-id-generator-framework` | 分布式 ID 生成（`UidGenerator`） |
| `damai-service-gray-transition-webmvc-framework` | 灰度发布过渡支持 |
| `damai-service-lock-framework` | 分布式锁支持 |

**依赖本模块（的客户端）的服务**：`damai-gateway-service`、`damai-user-service`、`damai-program-service` 均通过引入 `damai-base-data-client` 以 Feign 方式调用本服务的地区/渠道数据接口。

## 配置说明

配置文件位于 `src/main/resources`：`application.yml` 为主配置，`application-local.yml` / `application-pro.yml` 分别对应本地与生产环境（数据源、Nacos、Sentinel、Redis 等地址）。
