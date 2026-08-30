# damai-admin-service（微服务监控服务）

基于 Spring Boot Admin 的微服务监控中心，用于集中查看各业务微服务的运行状态、健康指标和 Actuator 端点信息，并在服务状态变化时通过钉钉机器人发送告警通知。

## 功能说明

- **服务监控中心**：通过 `@EnableAdminServer` 启用 Spring Boot Admin Server（`AdminApplication`），提供可视化的服务监控界面（UI 标题"服务监控中心"），自身也注册到 Nacos（`@EnableDiscoveryClient`）。
- **安全访问控制**：`SecurityConfig` 基于 Spring Security 配置登录页、静态资源放行、CSRF 忽略规则（`/instances`、`/actuator/**` 等），默认账号密码在 `application.yml` 中配置（admin/admin）。
- **状态变更告警**：`MonitorServer` 继承 `AbstractStatusChangeNotifier`，监听服务实例状态变化事件（UP/DOWN/OFFLINE 等），过滤掉恢复类的状态变更（如 `UNKNOWN:UP`、`DOWN:UP`），仅对异常变更用 SpEL 模板生成告警文案；`DingTalkMessage` 负责调用钉钉机器人 Webhook（`dingtalk.token` 配置）发送消息。
- **配置装配**：`MonitorServerConfig` 装配上述监控相关 Bean；`AdminConfig`（通过 `AutoConfiguration.imports` 自动装配）注册 Jackson 定制器（`JacksonCustomEnhance`）和 Knife4j 接口文档信息。

服务默认端口为 `10082`，应用名为 `damai-admin-service`。

## 技术栈

- Spring Boot Web / Validation / Actuator
- Spring Boot Admin Server（de.codecentric）
- Spring Security（监控页登录认证）
- Spring Cloud Alibaba Nacos Discovery（服务注册发现）
- springdoc-openapi + Knife4j（接口文档）
- fastjson、jackson-datatype-jsr310
- 项目内模块：`damai-common`（启动时排除了其自动装配 `DaMaiCommonAutoConfig`）

## 与其他模块的关系

- **依赖**：仅依赖项目内的 `damai-common`（通用工具/返回体），无其他项目内模块依赖，也不依赖任何 `damai-server-client` Feign 客户端包。
- **被依赖**：没有任何模块以 Maven 方式依赖本模块。它与其它业务服务是"运行时监控"关系——`damai-user-service`、`damai-program-service`、`damai-order-service`、`damai-pay-service`、`damai-base-data-service`、`damai-customize-service` 等通过引入 `spring-boot-admin-starter-client` 并暴露 Actuator 端点，将自身注册到本监控中心，由本模块统一展示状态并推送钉钉告警。
