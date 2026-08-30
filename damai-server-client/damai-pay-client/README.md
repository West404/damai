# damai-pay-client

支付服务（pay-service）的 Feign 客户端模块，隶属于 `damai-server-client`（各微服务的 RPC 接口包）。本模块本身不含业务实现，只提供支付服务对外暴露的远程调用接口、降级实现与传输对象（DTO/VO），供其他微服务以本地接口调用的方式远程调用支付服务。

## 功能说明

- `PayClient`：支付服务的 Feign 客户端接口，目标服务名为 `*-pay-service`（前缀由 `Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME` 控制，用于区分环境），声明了以下远程接口：
  - `commonPay(PayDto)`：`POST /pay/common/pay` 发起统一支付（支持支付宝、微信等渠道）
  - `notify(NotifyDto)`：`POST /pay/notify` 支付回调通知处理
  - `tradeCheck(TradeCheckDto)`：`POST /pay/trade/check` 查询交易支付状态
  - `refund(RefundDto)`：`POST /pay/refund` 发起退款
- `PayClientFallback`：`PayClient` 的熔断降级实现，调用异常时统一返回 `ApiResponse.error(BaseCode.SYSTEM_ERROR)`，避免异常向上扩散。
- 传输对象：
  - DTO：`PayDto`（支付入参）、`NotifyDto`（回调通知）、`TradeCheckDto`（交易状态查询）、`RefundDto`（退款）、`PayBillDto`（账单查询），均带 Jakarta Validation 校验注解和 Swagger `@Schema` 文档注解。
  - VO：`NotifyVo`（回调结果）、`TradeCheckVo`（交易状态结果）、`PayBillVo`（支付账单信息）。
- 所有接口返回统一的 `ApiResponse<T>`（来自 `damai-common`）。

## 技术栈

- Spring Cloud OpenFeign：声明式 HTTP 客户端，本模块的核心能力。
- feign-okhttp：Feign 底层使用 OkHttp 作为 HTTP 客户端。
- Spring Boot Validation（Jakarta Validation）：DTO 参数校验注解。
- Lombok、Swagger（springdoc `@Schema`）：简化代码与接口文档。
- 依赖项目内 `damai-common`（`ApiResponse`、`BaseCode`、`Constant` 等通用组件）与 `damai-service-common`。

## 与其他模块的关系

- **父模块**：`damai-server-client`（packaging=pom），统一管理各服务的 Feign 客户端子模块，公共依赖（OpenFeign、OkHttp、Validation、`damai-common` 等）在父 pom 中声明，本模块 pom 只定义自身坐标。
- **依赖的项目内模块**（继承自父 pom）：
  - `damai-common`：统一返回体 `ApiResponse`、错误码 `BaseCode`、常量 `Constant`。
  - `damai-service-common`（optional）：微服务通用组件。
- **对应的服务端**：`damai-server/damai-pay-service`，其 Controller 实现了本模块声明的 `/pay/**` 接口；pay-service 自身也依赖本模块以复用 DTO/VO 定义。
- **被依赖方**：`damai-server/damai-order-service`（订单服务）依赖本模块，通过 `PayClient` 调用支付服务完成下单支付、退款、支付状态查询等操作。
