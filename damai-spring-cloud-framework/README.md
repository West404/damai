# damai-spring-cloud-framework

微服务通用组件框架（父模块，`packaging=pom`），为大麦网仿站项目中 `damai-server` 下的各业务微服务提供统一的 Spring Cloud 基础能力，包括服务公共包、Web/Feign 组件、容器初始化框架和灰度发布框架。

## 功能说明

本模块自身不包含 Java 代码，仅聚合以下四个子模块：

| 子模块 | 分工 |
| --- | --- |
| `damai-service-common` | service 公共包：全局异常处理、分页工具、MyBatis-Plus 自动填充与分页插件、ShardingSphere 分库分表算法、Swagger/Knife4j 配置 |
| `damai-service-component` | service 组件：Feign 请求头透传（traceId/灰度标识/频道码）、请求体包装过滤器、Nacos 服务注册、Actuator + Micrometer 监控指标 |
| `damai-service-initialize` | service 容器初始化行为操作：四种初始化时机（PostConstruct、InitializingBean、ApplicationEvent、CommandLineRunner）的执行框架与组合模式执行器 |
| `damai-service-gray-transition-framework` | 灰度服务组件框架：基于 LoadBalancer 的灰度路由（base），并提供 Gateway 与 WebMvc 两种运行环境的上下文实现 |

各子模块均通过 `META-INF/spring/...AutoConfiguration.imports` 以 Spring Boot 自动装配方式生效，业务服务引入对应依赖即可，无需额外配置。

## 技术栈

- Spring Boot 3.3 / Spring Cloud 2023 / Spring Cloud Alibaba（Nacos 服务注册发现）
- Spring Cloud OpenFeign（feign-okhttp）、Spring Cloud LoadBalancer、Spring Cloud Gateway
- MyBatis-Plus、ShardingSphere-JDBC、PageHelper
- springdoc-openapi / Knife4j（接口文档）
- Micrometer + Prometheus（监控指标）、qps-helper（限流辅助）

## 与其他模块的关系

- **依赖的项目内模块**：各子模块均依赖 `damai-common`（通用工具、常量、异常、`ApiResponse` 返回体等）；灰度框架内部 `gateway`/`webmvc` 子模块依赖 `base` 子模块。
- **被依赖情况**：
  - `damai-server` 下的业务服务（user、base-data、customize、program、order、pay）普遍引入 `damai-service-common` 与 `damai-service-component`；
  - `damai-service-initialize` 被 user、program、pay 服务用于启动初始化逻辑；
  - `damai-service-gray-transition-webmvc-framework` 被 base-data、customize、program、order、pay 服务引入，`damai-service-gray-transition-gateway-framework` 被 `damai-gateway-service` 引入，共同构成全链路灰度能力；
  - `damai-server-client`（Feign 客户端包）依赖 `damai-service-common` 复用分页与公共 DTO 能力。
