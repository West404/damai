# damai-service-common

service 公共包，为各业务微服务提供 Web 层与持久层的通用基础能力。

## 功能说明

- **全局异常处理**：`DefaultExceptionHandler`（`@RestControllerAdvice`）统一捕获业务异常 `DaMaiFrameException`、参数校验异常和未捕获异常，返回统一的 `ApiResponse`。
- **MyBatis-Plus 配置**：`MybatisPlusAutoConfiguration` 注册分页插件（`PaginationInnerInterceptor`）和必须字段自动填充处理器 `MybatisPlusMetaObjectHandler`。
- **分页支持**：`PageUtil`、`PageVo`、`BasePageDto` 提供 MyBatis-Plus `IPage` 与 PageHelper `PageInfo` 两种分页结果的统一转换。
- **分库分表算法**：`DatabaseOrderComplexGeneArithmetic` / `TableOrderComplexGeneArithmetic` 实现 ShardingSphere 复合分片算法，按 `order_number` 或 `user_id` 路由订单库/表。
- **接口文档**：`SwaggerConfiguration` 提供 springdoc OpenAPI 基础配置（配合 Knife4j 展示）。
- **其他**：MQ 回调接口（`SuccessCallback`/`FailureCallback`）、`BaseTableData` 基础表数据初始化工具等。

自动装配入口：`META-INF/spring/...AutoConfiguration.imports` 中注册 `SwaggerConfiguration` 与 `MybatisPlusAutoConfiguration`。

## 技术栈

Spring Boot Web（optional）、MyBatis-Plus、ShardingSphere-JDBC、PageHelper、springdoc-openapi、Knife4j、Spring Validation。

## 与其他模块的关系

- **依赖**：项目内 `damai-common`（常量、枚举、异常、`ApiResponse` 等）。
- **被依赖**：`damai-server` 下 user、base-data、customize、program、order、pay 等业务服务，以及 `damai-server-client`（Feign 客户端包，复用分页与 DTO 能力）。
