# damai-service-initialize

service 容器初始化行为操作框架，将业务服务启动时需要执行的初始化逻辑抽象为统一的可排序、分时机执行的处理器体系。

## 功能说明

- **四种初始化时机**（`InitializeHandlerType`）：
  - `application_post_construct`：`@PostConstruct` 阶段；
  - `application_initializing_beans`：`InitializingBean` 阶段；
  - `application_event_listener`：监听 `ApplicationStartedEvent`；
  - `application_command_line_runner`：`CommandLineRunner` 阶段。
- **核心接口**：`InitializeHandler` 定义初始化类型 `type()`、执行顺序 `executeOrder()` 与执行逻辑 `executeInit()`；四种时机各有对应的抽象基类（如 `AbstractApplicationPostConstructHandler`）和执行器（如 `ApplicationPostConstructExecute`）。
- **统一装配**：`InitializeAutoConfig` 注册四个执行器，按类型收集所有 `InitializeHandler` 并按顺序执行。
- **组合模式执行器**：`AbstractComposite` / `CompositeContainer` / `CompositeInit` 支持将初始化逻辑组织成树形结构，按层级和顺序执行复杂初始化流程。

业务方只需继承对应时机的抽象 Handler 实现 `executeInit()` 即可接入。

## 技术栈

Spring Boot Web、项目内 `damai-common`。

## 与其他模块的关系

- **依赖**：项目内 `damai-common`。
- **被依赖**：`damai-server` 下 user、program、pay 服务（用于启动时加载基础数据、预热缓存等初始化逻辑）。
