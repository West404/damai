# damai-customize-client

定制服务（`damai-customize-service`）的客户端数据传输模块，artifactId 为 `damai-customize-client`，description 为 `rpc`。

## 功能说明

本模块是 `damai-server-client`（服务 Feign 接口包聚合模块）下的子模块，**只包含 DTO/VO 数据传输对象**，用于定制服务对外接口的参数与返回值定义，供服务提供方与调用方共享，保证双方数据结构一致。

包含的类：

- DTO（`com.damai.dto`）：
  - `RuleDto` / `RuleGetDto` / `RuleStatusDto` / `RuleUpdateDto`：普通限流规则的新增、查询、状态变更、更新参数
  - `DepthRuleDto` / `DepthRuleStatusDto` / `DepthRuleUpdateDto`：深度限流规则的相关参数
  - `AllRuleDto`：一次性添加全部规则的参数
  - `ApiDataDto`：API 被限制调用记录的分页查询参数（页码、起止日期、调用方 IP、API 路径）
  - `BroadcastCallDto`：广播调用参数（目标服务名、请求体）
- VO（`com.damai.vo`）：
  - `RuleVo` / `DepthRuleVo` / `AllDepthRuleVo`：规则查询返回结果
  - `ApiDataVo`：API 调用记录返回结果

所有 DTO/VO 使用 Swagger `@Schema` 注解描述字段（供 Knife4j 文档展示），并通过 `jakarta.validation` 注解（如 `@NotNull`、`@NotBlank`）做参数校验。

## 技术栈

本模块自身 pom 未显式声明依赖，全部继承自父模块 `damai-server-client`：

- Spring Cloud OpenFeign + feign-okhttp（Feign 调用基础设施，由父模块统一引入）
- spring-boot-starter-validation（Jakarta Validation 参数校验）
- Swagger / Knife4j 注解（`io.swagger.v3.oas.annotations`，来自 `damai-service-common`）
- Lombok（`@Data` 简化 POJO）
- Jackson（`@JsonFormat` 日期序列化）

## 与其他模块的关系

- **父模块**：`damai-server-client`（packaging=pom，聚合所有服务的 Feign 客户端包），同级的兄弟模块有 `damai-order-client`、`damai-base-data-client`、`damai-user-client`、`damai-job-client`、`damai-program-client`、`damai-pay-client`。
- **依赖的项目内模块**（经父模块传递）：
  - `damai-common`：通用工具、异常、统一返回体
  - `damai-service-common`（optional）：微服务通用组件（含 Swagger 注解等）
- **被哪些模块依赖**：经交叉确认，仅 `damai-server/damai-customize-service`（定制服务）在 pom 中引入了本模块。该服务的 `RuleController`、`DepthRuleController`、`AllRuleController`、`ApiDataController`、`BroadcastController` 直接使用本模块的 DTO/VO 作为接口出入参。

> 说明：与 `damai-user-client` 等模块不同，本模块当前只沉淀了数据传输对象，未定义 `@FeignClient` 接口；定制服务的对外接口由 `damai-customize-service` 的 Controller 直接暴露。
