# damai-common

公共基础模块，为整个大麦网仿站项目提供通用的工具类、统一返回体、异常体系、状态码枚举和自动装配配置。该模块**不依赖任何项目内模块**，是整个项目依赖链的最底层公共包。

## 功能说明

模块核心包结构位于 `com.damai` 下，主要能力如下：

- **统一返回体**：`common/ApiResponse` —— 接口响应规范结构（`code` / `message` / `data`），提供 `ok()`、`error()` 等静态工厂方法。
- **统一状态码**：`enums/BaseCode` —— 全项目通用的响应码枚举，涵盖系统异常、用户、节目、订单、支付、分布式锁、延迟队列等各业务域的错误码定义。
- **异常体系**：`exception/` 包下的 `BaseException`、`DaMaiFrameException`（业务异常，支持基于 `BaseCode` / `ApiResponse` 构造）、`ArgumentException` / `ArgumentError`（参数校验异常）。
- **通用枚举**：`enums/` 包下约 30 个业务枚举，如 `OrderStatus`、`PayChannel`、`SeatType`、`BusinessStatus`、`ApiRuleType` 等，供各服务统一引用。
- **Jackson 定制**：`config/DaMaiCommonAutoConfig` 通过 `META-INF/spring/...AutoConfiguration.imports` 自动装配，注册 `config/JacksonCustom`，统一 `Date` / `LocalDateTime` / `LocalDate` / `LocalTime` 的序列化格式（`yyyy-MM-dd HH:mm:ss` 等），并配置单引号、数字转字符串等序列化特性。
- **自定义日志布局**：`log4j/` 包下的 `DamaiJsonLayout`（Log4j2 插件，JSON 格式日志输出，支持 `projectName`、`env` 等扩展字段）、`BaseJsonLayout`、`JsonWriterBuilder`。
- **JWT 工具**：`jwt/TokenUtil` —— 基于 jjwt（HS256）的 token 生成与解析，过期时抛出 `DaMaiFrameException(TOKEN_EXPIRE)`。
- **线程上下文**：`threadlocal/BaseParameterHolder` —— 基于 `ThreadLocal` 的请求级参数存取（如 traceId、用户标识等基础参数的跨层传递）。
- **工具类**：`util/` 包下的 `RsaTool`、`RsaSignTool`（RSA 加解密/签名验签）、`Base64`、`DateUtils`、`StringUtil`、`RemoteUtil` 等；`core/SpringUtil`（Spring 容器 Bean 获取）、`environment/SpringEnvironment`（环境信息获取）。
- **常量**：`constant/Constant` —— 项目级通用常量。

## 技术栈

| 技术 | 用途 |
| --- | --- |
| Spring Boot 3.3（`spring-boot-starter-web`，optional） | Web 基础能力、自动装配 |
| Spring Cloud Commons（optional） | `InetUtils` 网络工具支持 |
| Log4j2（`spring-boot-starter-log4j2` 及 log4j-core/api 等） | 自定义 JSON 日志布局实现 |
| Jackson（jackson-databind） | JSON 序列化定制 |
| jjwt | JWT token 生成与解析 |
| Hutool（hutool-all） | 通用 Java 工具库 |
| fastjson | JSON 处理 |
| commons-codec / commons-lang | 编解码与基础工具 |
| springdoc-openapi（webmvc-ui） | `ApiResponse` 等的 OpenAPI/Knife4j 文档注解支持 |

## 与其他模块的关系

- **依赖关系**：本模块**不依赖**项目内任何其他模块，仅依赖第三方库，是项目的最底层公共包。
- **被依赖关系**：作为基础模块被项目中几乎所有模块直接依赖，包括：
  - 各基础框架：`damai-redis-tool-framework`（redis-common/redis/redis-stream 三个子模块）、`damai-redisson-framework`（redisson-common、service-delay-queue）、`damai-elasticsearch-framework`、`damai-id-generator-framework`、`damai-thread-pool-framework`、`damai-captcha-manage-framework`（base-captcha、captcha-framework）；
  - Spring Cloud 框架层：`damai-spring-cloud-framework` 下的 `damai-service-common`、`damai-service-component`、`damai-service-initialize` 及灰度相关子模块（gray-transition-base/webmvc/gateway）；
  - 客户端包：`damai-server-client`（供各服务 Feign 客户端使用统一返回体与枚举）；
  - 全部业务微服务：`damai-server` 下的 gateway、user、base-data、customize、program、order、pay、admin 等 9 个子模块。

上层服务通过依赖本模块获得统一的 `ApiResponse` 返回体、`BaseCode` 状态码、异常体系、业务枚举和 Jackson 序列化规范。
