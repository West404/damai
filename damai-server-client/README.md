# damai-server-client 客户端模块总览

## 功能说明

`damai-server-client` 是各业务微服务 Feign 客户端的聚合父模块（`packaging=pom`），为 `damai-server` 下的服务间远程调用提供统一的客户端接口包。每个子模块封装对应服务的：

- `@FeignClient` 声明式远程调用接口（如 `BaseDataClient`、`OrderClient`），通过服务名路由到目标服务；
- 对应的熔断降级实现类（如 `OrderClientFallback`），远程调用失败时返回 `ApiResponse.error(BaseCode.SYSTEM_ERROR)` 兜底响应；
- 调用所需的 DTO/VO 数据传输对象，接口出入参统一包装为 `ApiResponse<T>`。

服务提供方与调用方均依赖同一个 client 包，从而保证接口契约（路径、参数、返回值）一致。

## 技术栈

- Spring Cloud OpenFeign：声明式 HTTP 客户端
- feign-okhttp：Feign 底层 HTTP 连接池实现
- Spring Boot Validation：DTO 参数校验
- 内部依赖：`damai-common`（`ApiResponse` 统一返回体、枚举、常量）、`damai-service-common`（可选依赖）

## 子模块分工

| 子模块 | 核心类 | 说明 |
| --- | --- | --- |
| `damai-base-data-client` | `BaseDataClient` | 基础数据服务客户端：渠道数据、Token 数据、地区信息查询 |
| `damai-user-client` | `UserClient` | 用户服务客户端 |
| `damai-program-client` | `ProgramClient` | 节目服务客户端：节目、票档、座位等查询与操作（依赖 `damai-user-client` 复用其 DTO） |
| `damai-order-client` | `OrderClient` | 订单服务客户端：下单、取消、支付回调检查、订单统计等 |
| `damai-pay-client` | `PayClient` | 支付服务客户端：支付、退款、交易检查、支付结果通知 |
| `damai-job-client` | `JobClient` | 定时任务相关客户端：任务信息查询、执行与回调 |
| `damai-customize-client` | 无 Feign 接口 | 仅包含定制服务的 DTO/VO（普通规则、深度规则、接口数据、广播调用等），作为接口契约被依赖 |

## 与其他模块的关系

**依赖的项目内模块：**

- `damai-common`：统一返回体 `ApiResponse`、基础枚举与常量
- `damai-service-common`（optional）：微服务公共组件

**被以下服务依赖（见 `damai-server` 各服务 pom）：**

- `damai-gateway-service`：`damai-base-data-client`（网关鉴权查询 Token 数据）
- `damai-user-service`：`damai-base-data-client`、`damai-user-client`
- `damai-base-data-service`：`damai-base-data-client`（服务自身实现接口契约）
- `damai-customize-service`：`damai-customize-client`
- `damai-program-service`：`damai-base-data-client`、`damai-order-client`、`damai-program-client`
- `damai-order-service`：`damai-order-client`、`damai-pay-client`、`damai-program-client`
- `damai-pay-service`：`damai-pay-client`

各服务通过引入对应 client 包并注入 `XxxClient` 接口即可发起跨服务调用，配合 Nacos 服务发现完成负载均衡。
