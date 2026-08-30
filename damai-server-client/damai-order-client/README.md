# damai-order-client

订单服务（`damai-order-service`）的 Feign 客户端模块，供其他微服务通过 RPC 方式调用订单服务接口。

## 功能说明

本模块本身不含业务实现，主要包含三部分内容：

- **Feign 接口**：`com.damai.client.OrderClient`
  - 通过 `@FeignClient(value = "...-order-service", fallback = OrderClientFallback.class)` 声明远程调用，服务名前缀由 `damai-common` 中的 `Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME` 拼接而成（用于区分不同环境注入的服务）。
  - 暴露的接口：
    - `POST /order/create`：创建订单（`create(OrderCreateDto)`）
    - `POST /order/account/order/count`：查询账户下某个节目的订单数量（`accountOrderCount(AccountOrderCountDto)`）
- **降级处理**：`OrderClientFallback`，实现 `OrderClient` 全部方法，远程调用失败时统一返回 `ApiResponse.error(BaseCode.SYSTEM_ERROR)`，配合熔断降级使用。
- **共享传输对象**：`com.damai.dto` / `com.damai.vo` 包下与订单服务契约一致的 DTO/VO，例如 `OrderCreateDto`、`OrderCancelDto`、`OrderGetDto`、`OrderListDto`、`OrderPayDto`、`OrderPayCheckDto`、`OrderRefundDto`、`OrderTicketUserCreateDto`、`AccountOrderCountDto` 以及 `OrderGetVo`、`OrderListVo`、`OrderPayCheckVo`、`OrderTicketUserVo`、`UserInfoVo` 等，供订单服务与调用方共用，保证接口参数/返回体一致。

## 技术栈

依赖在父模块 `damai-server-client` 中统一声明，本模块直接继承：

- Spring Cloud OpenFeign（`spring-cloud-starter-openfeign`）：声明式 HTTP 远程调用
- `feign-okhttp`：Feign 的 OkHttp 底层传输
- Spring Boot Validation（`spring-boot-starter-validation`）：DTO 参数校验注解
- 统一返回体 `ApiResponse`、错误码 `BaseCode` 等来自项目内 `damai-common`

## 与其他模块的关系

依赖（经父模块 `damai-server-client` 引入）：

- `damai-common`：通用返回体 `ApiResponse`、错误码枚举、常量 `Constant` 等
- `damai-service-common`（optional）：微服务通用组件

被依赖：

- `damai-order-service`（订单服务自身）：复用本模块的 DTO/VO 作为 Controller 的入参/出参契约
- `damai-program-service`（节目服务）：在下单流程（如 `ProgramOrderService` 及各版本下单策略 `ProgramOrderV1Strategy` ~ `ProgramOrderV4Strategy`）中通过 `OrderClient` 远程调用订单服务创建订单、查询账户订单数

> 注：项目内各 client 模块统一使用 `com.damai` 根包，因此 `com.damai.dto` / `com.damai.vo` 下的类按职责分散在不同 client 模块中，引用时以所在 Maven 模块为准。
