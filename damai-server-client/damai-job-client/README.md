# damai-job-client

job 任务调度服务的 Feign 客户端模块（`rpc`），为项目内其他微服务提供调用 job 服务的 RPC 接口定义与数据传输对象（DTO）。本模块不含业务实现，只包含 Feign 接口、降级类和 DTO，作为独立的 jar 包供需要的业务服务引入。

## 功能说明

- `JobClient`：基于 `@FeignClient` 声明的 job 服务远程调用接口，服务名为 `${prefix}-job-service`（前缀由 `Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME` 配置注入，用于环境/灰度区分）。目前提供：
  - `callBack(JobCallBackDto)`：job 任务执行完成后，向 job 服务的 `jobRunRecord/callBack` 接口上报任务执行状态（成功/失败、剩余重试次数等）。
- `JobClientFallback`：`JobClient` 的降级实现，远程调用失败时返回 `ApiResponse.error(BaseCode.SYSTEM_ERROR)`，避免异常向上游扩散。
- DTO 模型（`com.damai.dto` 包）：
  - `JobCallBackDto`：job 执行结果回调参数（任务 id、执行信息、执行状态、剩余重试次数）。
  - `JobInfoDto`：job 任务定义（名称、描述、url、请求头、请求方法、参数、是否重试及重试次数）。
  - `JobInfoPageDto`：job 任务分页查询参数。
  - `RunJobDto`：手动触发执行某个 job 任务的参数。

所有 DTO 均使用 `jakarta.validation` 做参数校验，并用 Swagger（`@Schema`）注解描述字段，便于 Knife4j 文档展示。

## 技术栈

- Spring Cloud OpenFeign：声明式 RPC 调用。
- Feign + OkHttp（`feign-okhttp`）：HTTP 客户端。
- Spring Boot Validation（`jakarta.validation`）：接口入参校验。
- Lombok：简化 DTO 代码。
- Swagger / springdoc（`io.swagger.v3.oas.annotations`）：接口文档注解。

> 说明：本模块自身 pom 未声明额外依赖，以上依赖由父模块 `damai-server-client` 统一引入。

## 与其他模块的关系

### 依赖的项目内模块（继承自父模块 `damai-server-client`）

- `damai-common`：提供统一返回体 `ApiResponse`、错误码 `BaseCode`、常量 `Constant` 等基础设施。
- `damai-service-common`（optional）：微服务通用组件。

### 被哪些模块依赖

- 当前仓库内暂无其他模块在 pom 中直接声明依赖 `damai-job-client`，也暂无业务代码调用 `JobClient`。它是为 job 任务调度场景预留的客户端契约包：对应的 `job-service`（任务调度服务）独立部署，不包含在本仓库 `damai-server` 的 9 个业务子模块中。业务服务需要上报任务执行结果时，引入本模块并注入 `JobClient` 即可。
