# damai-base-data-client

基础数据服务（`base-data-service`）的 Feign 客户端模块，artifactId 为 `damai-base-data-client`，属于 `damai-server-client` 聚合工程下的 RPC 接口包。

## 功能说明

本模块不承载业务逻辑，只定义基础数据服务对外暴露的 RPC 契约，供其他微服务通过 Feign 调用，核心内容：

- `BaseDataClient`：基础数据服务的 Feign 客户端接口（`@FeignClient`，服务名为 `base-data-service`），声明了以下远程接口：
  - `getByCode`：根据 code 查询渠道数据
  - `get`：查询 token 数据
  - `selectByIdList`：根据 id 集合批量查询地区列表
  - `getById`：根据 id 查询地区
- `BaseDataClientFallback`：上述接口的熔断降级实现，调用失败时统一返回 `ApiResponse.error(BaseCode.SYSTEM_ERROR)`
- `com.damai.dto` 包：接口入参对象，如 `GetChannelDataByCodeDto`、`AreaSelectDto`、`AreaGetDto`、`ChannelDataAddDto`、`TokenDataDto`
- `com.damai.vo` 包：接口出参对象，如 `GetChannelDataVo`、`TokenDataVo`、`AreaVo`

所有接口的返回体均包装为 `damai-common` 中的 `ApiResponse`。

## 技术栈

依赖由父模块 `damai-server-client` 统一定义，本模块自身无额外依赖：

- Spring Cloud OpenFeign：声明式 RPC 调用
- feign-okhttp：Feign 底层 HTTP 客户端
- spring-boot-starter-validation：DTO 参数校验
- `damai-common`：通用返回体 `ApiResponse`、错误码 `BaseCode`、常量等
- `damai-service-common`（optional）：微服务公共组件

## 与其他模块的关系

- **父模块**：`damai-server-client`（聚合各服务的 Feign 客户端包）
- **依赖的项目内模块**（继承自父模块 pom）：`damai-common`、`damai-service-common`
- **被以下模块依赖**：
  - `damai-base-data-service`：基础数据服务自身（共享 DTO/VO 契约）
  - `damai-user-service`：用户服务，通过该客户端查询渠道、地区等基础数据
  - `damai-program-service`：节目服务，通过该客户端获取地区等基础数据
  - `damai-gateway-service`：网关服务，通过该客户端查询 token 等数据
