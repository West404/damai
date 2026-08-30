# damai-thread-pool-framework

线程池封装模块（artifactId: `damai-thread-pool-framework`），为业务微服务提供统一的异步任务线程池，并保证异步任务执行时能够延续全链路的 `traceId`（日志链路追踪）上下文。

## 功能说明

- **`BusinessThreadPool`**：业务线程池入口类（静态方法 `execute` / `submit`）。内部基于 JDK `ThreadPoolExecutor` 构建，核心线程数为 CPU 核数 + 1，最大线程数约为核数的 5 倍，队列容量 600（`ArrayBlockingQueue`）。
- **`BaseThreadPool`**（`base` 包）：线程池基类，负责在任务提交时捕获父线程的 MDC 日志上下文（`traceId`）与 `BaseParameterHolder` 请求参数上下文，在任务执行前注入、执行后还原，保证异步线程不串上下文。
- **`BusinessNameThreadFactory` / `AbstractNameThreadFactory`**（`namefactory` 包）：自定义线程工厂，为线程生成可读的名称（如 `task-pool--1--thread--2`），便于日志排查。
- **`ThreadPoolRejectedExecutionHandler.BusinessAbortPolicy`**（`rejectedexecutionhandler` 包）：拒绝策略，任务被拒绝时抛出包含线程池信息的 `RejectedExecutionException`。
- **`RequestParamContextFilter` / `FilterConfig`**（`filter` 包）：请求过滤器，从请求头读取 `traceId` 放入 MDC，请求结束后清理。`FilterConfig` 通过 `META-INF/spring/...AutoConfiguration.imports` 注册为 Spring Boot 自动装配配置，引入本模块即自动生效。

## 技术栈

- Spring Boot 3.3（`spring-boot-starter-web`，optional，提供过滤器支持，基于 Jakarta Servlet）
- JDK 并发包（`ThreadPoolExecutor`、`ArrayBlockingQueue`）
- SLF4J MDC（日志链路 `traceId` 传递）

## 与其他模块的关系

- **依赖**：
  - `damai-common`：使用其中的 `BaseParameterHolder`（请求参数上下文持有器）、`Constant.TRACE_ID` 常量、`StringUtil` 工具类。
- **被依赖**：通过 grep 各模块 pom 确认，以下业务微服务引入了本模块：
  - `damai-server/damai-user-service`（用户服务）
  - `damai-server/damai-order-service`（订单服务）
  - `damai-server/damai-program-service`（节目服务）

引入方式：在业务服务的 pom 中加入本模块依赖后，`FilterConfig` 自动装配生效，代码中直接调用 `BusinessThreadPool.execute(...)` / `submit(...)` 即可提交带链路上下文的异步任务。
