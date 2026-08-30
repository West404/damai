# damai-captcha-framework

验证码的 Spring Boot 自动装配封装层，引入即自动注册验证码相关的 Bean 和 HTTP 接口，供业务微服务直接使用。

## 功能说明

- **自动装配**：通过 `META-INF/spring/...AutoConfiguration.imports` 注册：
  - `AjCaptchaAutoConfiguration`：入口配置，开启 `aj.captcha` 配置属性（`AjCaptchaProperties`），并导入服务/存储自动配置；
  - `AjCaptchaServiceAutoConfiguration`：根据配置创建 `CaptchaService`（支持加载 classpath 自定义底图）；
  - `AjCaptchaStorageAutoConfiguration`：按 `cacheType`（redis/local）创建 `CaptchaCacheService`；
  - `CaptchaAutoConfig`：注册 `CaptchaHandle` 及基于 Redis 的缓存实现（`@Primary` 覆盖默认 local 实现）。
- **HTTP 接口**：`CaptchaController` 暴露 `/captcha/get`（获取验证码）和 `/captcha/check`（校验验证码），自动将客户端 IP + UA 作为 `browserInfo` 参与校验。
- **编程式调用**：`CaptchaHandle` 提供 `getCaptcha`、`checkCaptcha`、`verification` 方法，业务代码可直接注入使用。
- **缓存实现**：`CaptchaCacheServiceRedisImpl` 基于 `StringRedisTemplate` 的验证码数据 Redis 缓存。
- **配置项**：前缀 `aj.captcha`，支持验证码类型、水印、滑块误差、AES 开关、缓存类型、接口频率限制等（详见 `AjCaptchaProperties`）。

## 技术栈

- Java 17、Spring Boot 3（`spring-boot-starter-web`、`spring-boot-starter-data-redis`）、Redis。

## 与其他模块的关系

- 依赖项目内模块：`damai-common`、`damai-base-captcha`（验证码核心实现）。
- 被依赖：`damai-user-service`（用户服务），用于用户注册/登录等场景的验证码校验（如 `UserCaptchaController`、`UserRegisterVerifyCaptcha`）。
