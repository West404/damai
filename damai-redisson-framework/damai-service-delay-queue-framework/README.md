# damai-service-delay-queue-framework

基于 Redisson `RDelayedQueue` 的延迟队列工具，用于订单超时关单等延迟任务场景。

## 功能说明

- `DelayProduceQueue`：延迟消息生产者，封装 `RDelayedQueue.offer(content, delayTime, timeUnit)`。
- `DelayConsumerQueue` / `ConsumerTask`：延迟消息消费者，从阻塞队列中拉取到期的消息并处理。
- `IsolationRegionSelector`：分片选择器，通过轮询计数将消息分散到多个隔离分区，降低单队列压力。
- `DelayQueueContext` / `DelayQueueBasePart` / `DelayQueuePart` / `DelayQueueProduceCombine`：延迟队列的上下文与组合管理，统一维护生产者、消费者与分区的生命周期。
- `DelayQueueAutoConfig` / `DelayQueueProperties`：自动装配与配置项；`DelayQueueInitHandler` 在应用启动后初始化队列消费。

## 技术栈

- Spring Boot 3（自动装配）
- Redisson（`RDelayedQueue` / `RBlockingQueue`，经由 `damai-redisson-common-framework`）

## 与其他模块的关系

- 依赖：`damai-redisson-common-framework`（`RedissonClient`）、`damai-common`。
- 被依赖：业务微服务 `damai-program-service`、`damai-order-service`（如订单创建后延时关单、取消未支付订单等延迟任务）。
