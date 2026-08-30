# damai-user-client

用户服务（user-service）的 Feign 客户端包，供项目内其他微服务以 RPC 方式调用用户相关接口。

## 功能说明

本模块是纯客户端模块（`packaging=jar`，无启动类、无业务实现），核心内容：

- `UserClient`：基于 Spring Cloud OpenFeign 的声明式客户端，指向 Nacos 中注册的用户服务（服务名 `${prefix.distinction.name:damai}-user-service`，前缀常量定义在 `damai-common` 的 `Constant` 中）。提供三个接口：
  - `getById(UserIdDto)`：根据用户 id 查询用户信息（`/user/getById`）
  - `list(TicketUserListDto)`：根据 userId 查询该用户下的购票人列表（`/ticket/user/list`）
  - `getUserAndTicketUserList(UserGetAndTicketUserListDto)`：同时查询用户信息及其购票人集合（`/user/get/user/ticket/list`）
- `UserClientFallback`：Feign 调用失败时的降级实现，统一返回 `ApiResponse.error(BaseCode.SYSTEM_ERROR)`，避免调用方直接抛出远程调用异常。
- `com.damai.dto` / `com.damai.vo`：与 user-service 接口对应的请求 DTO（如 `UserIdDto`、`TicketUserListDto`、`UserLoginDto`、`UserRegisterDto` 等）和响应 VO（如 `UserVo`、`TicketUserVo`、`UserLoginVo` 等），供服务提供方与调用方共享。

## 技术栈

本模块自身不直接声明依赖，由父模块 `damai-server-client` 统一引入：

- Spring Cloud OpenFeign：声明式 HTTP RPC 客户端
- feign-okhttp：Feign 底层 HTTP 连接池实现
- spring-boot-starter-validation：DTO 参数校验（`@NotNull` 等注解）
- 统一返回体 `ApiResponse`、错误码 `BaseCode` 来自项目内模块 `damai-common`

## 与其他模块的关系

依赖关系（本模块被谁用）：

- 本模块自身的 pom 仅继承父模块 `damai-server-client`，实际代码依赖 `damai-common`（`ApiResponse`、`BaseCode`、`Constant`）和 `damai-service-common`（父 pom 引入，optional）。
- `damai-program-client`（节目服务客户端包）直接依赖本模块，因此 `damai-program-service` 与 `damai-order-service` 均通过 `damai-program-client` 间接获得 `UserClient`，用于下单/节目校验时查询用户与购票人信息（如 `ProgramUserExistCheckHandler`、`OrderService`、`ProgramService` 中注入了 `UserClient`）。
- `damai-user-service`（用户服务实现方）也依赖本模块，复用其中的 DTO/VO 作为自身接口的出入参定义，保证调用方与服务方契约一致。

父模块 `damai-server-client` 下还有 `damai-order-client`、`damai-base-data-client`、`damai-job-client`、`damai-customize-client`、`damai-program-client`、`damai-pay-client` 等兄弟模块，分别对应各业务服务的 Feign 客户端。
