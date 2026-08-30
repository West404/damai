# damai-service-gray-transition-framework

灰度服务组件框架（父模块，`packaging=pom`），基于 Spring Cloud LoadBalancer 实现按灰度标识路由服务实例的全链路灰度发布能力。

## 功能说明

本模块自身无代码，聚合三个子模块：

| 子模块 | 分工 |
| --- | --- |
| `damai-service-gray-transition-base-framework` | 灰度基础组件：核心过滤逻辑与负载均衡增强，与运行环境（WebMvc/WebFlux）无关 |
| `damai-service-gray-transition-gateway-framework` | 灰度 Gateway 组件：Spring Cloud Gateway（响应式）环境下的上下文实现 |
| `damai-service-gray-transition-webmvc-framework` | 灰度 WebMvc 组件：传统 Servlet 环境下的上下文实现 |

### base 子模块核心类

- `ServerGrayFilter`：灰度过滤器，根据请求中的灰度标识与 Nacos 实例元数据（`server-gray`）筛选目标实例列表；
- `FilterLoadBalance` / `DefaultFilterLoadBalance`：服务实例过滤链接口与默认实现，串联所有 `AbstractServerFilter`；
- `ExtCachingServiceInstanceListSupplier` / `EnhanceServiceInstanceListSupplierBuilder`：对 LoadBalancer 的 `CachingServiceInstanceListSupplier` 增强，在实例列表返回前应用灰度过滤；
- `GrayLoadBalanceAutoConfiguration`：灰度负载均衡自动装配入口；
- `ContextHandler`：从请求头取值的上下文抽象，由 gateway/webmvc 子模块分别实现。

### gateway 子模块核心类

- `GatewayWorkRouteFilter` / `GatewayWorkClearFilter`：全局过滤器，分别在请求链头/尾将 `ServerWebExchange` 存入、清除 `GatewayContextHolder`；
- `GatewayContextHandler`：基于 `GatewayContextHolder` 从请求头读取灰度标识。

### webmvc 子模块核心类

- `WebMvcContextHandler`：基于 `RequestContextHolder` 从 `HttpServletRequest` 请求头读取灰度标识。

## 技术栈

Spring Cloud LoadBalancer、Spring Cloud Alibaba Nacos Discovery、Spring Cloud Gateway（gateway 子模块）、Spring WebFlux / spring-retry（base，optional）、Spring Boot Web（webmvc 子模块）。

## 与其他模块的关系

- **依赖**：各子模块均依赖项目内 `damai-common`（常量、异常、工具类）；gateway 与 webmvc 子模块依赖 base 子模块。
- **被依赖**：
  - `damai-service-gray-transition-gateway-framework` 被 `damai-server/damai-gateway-service`（网关）引入；
  - `damai-service-gray-transition-webmvc-framework` 被 base-data、customize、program、order、pay 业务服务引入；
  - 与 `damai-service-component` 的 `FeignRequestInterceptor`（灰度请求头透传）配合，实现"网关 → 业务服务 → 下游服务"的全链路灰度路由。
