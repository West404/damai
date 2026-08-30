# damai-redis-stream-framework

Redis Stream 消息队列封装模块，基于 Redis Stream 提供轻量级的消息发布/订阅能力，作为 Kafka 之外的一种消息通道选择。

## 功能说明

- `RedisStreamPushHandler`：消息生产者，向指定 Stream 推送消息
- `RedisStreamHandler`：Stream 操作封装，负责消费组创建（`streamBindingGroup`）、消息删除等
- `RedisStreamListener`：消息监听器，将收到的 Stream 消息转发给业务消费者
- `MessageConsumer`：业务消费接口，由使用方实现具体消费逻辑；存在该 Bean 时才启动监听容器（`@ConditionalOnBean`）
- `RedisStreamConfigProperties`：配置属性（前缀 `spring.data.redis.stream`），支持配置 `streamName`、`consumerGroup`、`consumerName` 及消费方式 `consumerType`（`group` 消费组 / `broadcast` 广播）
- `RedisStreamAutoConfig`：自动装配类，构建 `StreamMessageListenerContainer`（内置消费线程池），按消费方式绑定消费组或广播监听；通过 `AutoConfiguration.imports` 自动生效

## 技术栈

- Spring Boot 3.x（`spring-boot-starter-web`、`spring-boot-starter-data-redis`）
- Spring Data Redis Stream（`StreamMessageListenerContainer`）
- Fastjson、Lombok

## 与其他模块的关系

- 依赖项目内模块：`damai-common`、`damai-redis-common-framework`
- 被依赖方：`damai-server` 下的 `damai-program-service`，用于节目相关数据的 Redis Stream 消息生产与消费
