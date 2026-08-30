# damai-repeat-execute-limit-framework

防重复执行（接口幂等）工具，基于分布式锁 + 本地锁实现，防止同一请求被重复提交/消费。

## 功能说明

- `@RepeatExecuteLimit` 注解：标注在方法或类上，可配置业务名（`name`）、SpEL 锁 key（`keys`）、幂等保持时长（`durationTime`，不配置则以方法执行时间为准）以及触发限制时的提示信息（`message`，默认"提交频繁，请稍后重试"）。
- `RepeatExecuteLimitAspect`：AOP 切面，进入方法前尝试获取锁，获取失败即判定为重复请求并抛出提示。
- `RepeatExecuteLimitLockInfoHandle`：实现 `LockInfoHandle`，负责按注解配置解析生成幂等锁 key。
- 幂等锁名前缀等常量见 `RepeatExecuteLimitConstants`。

## 技术栈

- Spring Boot 3（AOP）
- Redisson（经由 `damai-redisson-common-framework`）

## 与其他模块的关系

- 依赖：`damai-redisson-common-framework`、`damai-service-lock-framework`（复用分布式锁实现幂等控制）。
- 被依赖：业务微服务 `damai-customize-service`、`damai-program-service`、`damai-order-service`（如下单、回调等需幂等的接口）。
