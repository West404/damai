# damai-user-service（用户服务）

大麦网仿站项目中的用户微服务，默认端口 `6082`，应用名 `damai-user-service`，注册到 Nacos。启动类为 `com.damai.UserApplication`。

## 功能说明

负责本系统的用户域全部能力：

- **用户管理**（`UserController`，路径 `/user`）：注册、登录/退出登录（JWT Token + Redis 会话）、按手机号/ID 查询、修改个人信息/密码/邮箱/手机号、实名认证、用户是否存在校验，以及仅供内部服务调用的“用户+购票人列表”查询。
- **购票人管理**（`TicketUserController`，路径 `/ticket/user`）：购票人的列表查询、添加、删除。
- **图形验证码**（`UserCaptchaController`，路径 `/user/captcha`）：基于 `damai-captcha-framework` 的验证码获取与校验，并通过 Lua 脚本（`CheckNeedCaptchaOperate` + `lua/checkNeedCaptcha.lua`）结合本地计数器 `RequestCounter` 判断当前请求量是否需要开启验证码。

核心实现类：

- `UserService`：注册/登录/资料修改等核心逻辑。注册使用 `@ServiceLock` 分布式读写锁防止并发重复注册，登录失败次数通过 Redis 计数限流，Token 由 `TokenUtil`（JWT）生成，渠道密钥通过 `BaseDataClient`（Feign 调用 base-data 服务）获取并走 Redis 缓存。
- 注册校验责任链：`composite/register/` 下的 `AbstractUserRegisterCheckHandler` 及其实现（`UserExistCheckHandler` 用户是否存在、`UserRegisterCountCheckHandler` 注册频率、`UserRegisterVerifyCaptcha` 验证码校验），由 `CompositeContainer` 组合执行。
- `UserBloomFilterInitData`：服务启动时将全量用户手机号预热进布隆过滤器，注册/存在性校验先过滤布隆再查库。
- 数据访问：`User` / `UserMobile` / `UserEmail` / `TicketUser` 四张表，对应 `UserMapper`、`UserMobileMapper`、`UserEmailMapper`、`TicketUserMapper`（MyBatis-Plus）。

**分库分表与数据加密**：数据源使用 ShardingSphere（`shardingsphere-user-*.yaml`），四张表均按 2 库 2 表拆分（`d_user` 按 `id`、`d_user_mobile` 按 `mobile`、`d_user_email` 按 `email`、`d_ticket_user` 按 `user_id` 取模路由），手机号、密码、身份证号等敏感字段使用 SM4 算法落库加密。

## 技术栈

- Spring Boot Web + Validation、Log4j2、Spring Cloud OpenFeign（OkHttp）+ LoadBalancer
- Spring Cloud Alibaba Nacos（服务注册发现）
- MySQL + MyBatis-Plus + ShardingSphere（分库分表 + SM4 字段加密）
- Redis（缓存、Lua 脚本、登录错误计数）+ Redisson（分布式锁、布隆过滤器）
- 百度 UidGenerator 分布式 ID（`damai-id-generator-framework`）
- JWT（Token 生成/解析）、图形验证码（aj-captcha 体系）
- Jasypt 配置加密、Spring Boot Actuator + Spring Boot Admin Client 监控
- Knife4j / SpringDoc 接口文档

## 与其他模块的关系

依赖的项目内模块（见 `pom.xml`）：

- `damai-common`：通用工具类、异常、统一返回体 `ApiResponse`
- `damai-service-common`、`damai-service-component`、`damai-service-initialize`（`damai-spring-cloud-framework` 下）：微服务公共组件、组合校验容器 `CompositeContainer`、启动初始化执行器
- `damai-service-lock-framework`、`damai-bloom-filter-framework`（`damai-redisson-framework` 下）：`@ServiceLock` 分布式读写锁、布隆过滤器
- `damai-redis-framework`（`damai-redis-tool-framework` 下）：`RedisCache` 缓存封装
- `damai-id-generator-framework`：UidGenerator 分布式 ID
- `damai-captcha-framework`（`damai-captcha-manage-framework` 下）：图形验证码
- `damai-thread-pool-framework`：`BusinessThreadPool` 业务线程池
- `damai-user-client`：本服务对外的 Feign 客户端包（DTO/VO 契约）
- `damai-base-data-client`：Feign 调用 base-data 服务获取渠道数据（Token 密钥）

被依赖情况：本模块是可独立部署的应用，其他服务不直接依赖其 jar；服务间通过 `damai-user-client` 的 Feign 接口远程调用本服务（如 `damai-program-client` 依赖了 `damai-user-client`）。前端流量经 `damai-gateway-service` 网关路由到本服务。

## 目录结构

```
src/main/java/com/damai
├── UserApplication.java            # 启动类
├── controller/                     # 用户/购票人/验证码 控制层
├── service/                        # 业务逻辑
│   ├── composite/register/         # 注册校验责任链
│   ├── init/                       # 布隆过滤器启动预热
│   ├── lua/                        # 验证码触发判断 Lua 脚本执行器
│   └── tool/                       # 本地请求计数器
├── entity/                         # 用户/手机号/邮箱/购票人 实体
└── mapper/                         # MyBatis-Plus Mapper
src/main/resources
├── application.yml                 # 服务配置（端口 6082）
├── shardingsphere-user-*.yaml      # 分库分表 + 字段加密配置（local/docker/pro）
└── lua/checkNeedCaptcha.lua        # 是否需要验证码判断脚本
```
