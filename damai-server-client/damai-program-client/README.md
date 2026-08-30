# damai-program-client

节目服务（program-service）的 Feign 客户端包。本模块不承载业务逻辑，只向其他微服务提供调用 `damai-program-service` 所需的远程接口定义、熔断降级实现，以及节目域的 DTO/VO 数据传输对象，属于典型的"客户端 SDK"模块。

## 功能说明

- **Feign 远程调用接口**：
  - `ProgramClient`：基于 `@FeignClient` 声明对 `program-service` 的远程调用，当前提供 `selectListByProgram`（按节目查询票档集合）接口。
  - `ProgramClientFallback`：对应的降级实现，调用失败时返回 `ApiResponse.error(BaseCode.SYSTEM_ERROR)`，避免异常直接向上游扩散。
- **节目域数据传输对象**：
  - `dto` 包：节目、节目类型、票档、座位、演出时间等相关入参对象，如 `ProgramAddDto`、`ProgramPageListDto`、`ProgramSearchDto`、`ProgramOrderCreateDto`、`TicketCategoryDto`、`SeatBatchAddDto`、`ProgramShowTimeAddDto`、`DelayOrderCancelDto` 等。
  - `vo` 包：节目及关联信息的出参对象，如 `ProgramVo`、`ProgramHomeVo`、`ProgramGroupVo`、`ProgramCategoryVo`、`TicketCategoryVo`、`SeatVo`、`ProgramShowTimeVo` 等。
- 这些 DTO/VO 被 `program-service` 自身（Controller 入参/出参）和其他服务（如订单服务）共用，保证服务间数据结构一致。

## 技术栈

- Spring Cloud OpenFeign：声明式 HTTP 远程调用（含 `feign-okhttp` 底层客户端，由父模块统一引入）。
- Spring Boot Validation：DTO 参数校验注解支持。
- 项目公共依赖（由父模块 `damai-server-client` 统一引入）：`damai-common`（`ApiResponse` 统一返回体、`BaseCode` 枚举、常量等）、`damai-service-common`（微服务通用组件）。

## 与其他模块的关系

**本模块依赖：**

- `damai-user-client`：节目域部分数据结构复用用户客户端中的定义，同时间接获得 Feign 基础设施。
- `damai-common`、`damai-service-common`：经父模块 `damai-server-client` 传递引入。

**被以下模块依赖：**

- `damai-program-service`：作为自身对外接口的契约包，Controller 直接使用本模块的 DTO/VO。
- `damai-order-service`：创建订单等流程中需要节目域的数据结构（如 `ProgramOrderCreateDto`）并远程调用节目服务。
