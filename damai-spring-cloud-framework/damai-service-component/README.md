# damai-service-component

service 组件包，为业务微服务提供 Feign 增强、请求过滤器、服务注册与监控等 Web 层通用组件。

## 功能说明

- **Feign 增强**：
  - `FeignRequestInterceptor`：Feign 调用时从当前请求透传 `traceId`、`code`（频道码）、灰度标识等请求头，灰度标识缺省时回落到本服务的 `server.gray` 配置；
  - `ExtraFeignAutoConfiguration`：上述拦截器的自动装配配置（基于 OkHttp 作为 Feign 客户端）。
- **请求过滤器**：
  - `RequestWrapperFilter` + `CustomizeRequestWrapper`：缓存请求体，使 body 可重复读取；
  - `BaseParameterFilter`：解析请求头中的 `traceId`、灰度标识、`userId`、`code`，写入 `BaseParameterHolder`（ThreadLocal）与日志 MDC，并打印请求日志；
  - `FilterAutoConfiguration`：过滤器自动装配。
- **监控与注册**：集成 Actuator、Micrometer（Prometheus 指标、JVM extras）、qps-helper，以及 Nacos 服务注册发现。

自动装配入口：`META-INF/spring/...AutoConfiguration.imports` 中注册 `ExtraFeignAutoConfiguration` 与 `FilterAutoConfiguration`。

## 技术栈

Spring Boot Web、Spring Cloud OpenFeign（feign-okhttp）、Spring Cloud Alibaba Nacos Discovery、Spring Boot Actuator、Micrometer（prometheus / core / jvm-extras）、qps-helper。

## 与其他模块的关系

- **依赖**：项目内 `damai-common`（常量、`BaseParameterHolder`、工具类等）。
- **被依赖**：`damai-server` 下 user、base-data、customize、program、order、pay 等业务服务。
