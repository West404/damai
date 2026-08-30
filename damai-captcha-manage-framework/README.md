# damai-captcha-manage-framework

图形验证码管理框架（父模块，`packaging=pom`），为整个大麦网仿站项目提供行为式验证码能力（滑动拼图、旋转拼图、文字点选）。代码基于开源的 AJ-Captcha（anji-plus）改造而来。

## 功能说明

- 提供验证码的生成（`get`）、校验（`check`）、二次验证（`verification`）完整流程；
- 验证码底图绘制、坐标 AES 加密、接口频率限制等安全能力；
- 验证码数据支持本地内存（local）或 Redis 两种缓存方式；
- 以 Spring Boot Starter 自动装配的形式供业务服务引入，业务服务引入后自动暴露 `/captcha/get`、`/captcha/check` 接口，也可通过 `CaptchaHandle` 编程式调用。

## 子模块分工

| 子模块 | 说明 |
| --- | --- |
| `damai-base-captcha` | 验证码核心基础包：模型（`CaptchaVO`、`CaptchaTypeEnum`）、核心服务接口与实现（`CaptchaService`、`BlockPuzzleCaptchaServiceImpl`、`ClickWordCaptchaServiceImpl`、`RotatePuzzleCaptchaServiceImpl`）、工厂（`CaptchaServiceFactory`）、本地缓存实现（`CaptchaCacheServiceMemImpl`）、频率限制（`FrequencyLimitHandler`）及各类工具类（`AesUtil`、`ImageUtils` 等）。不依赖 Spring，可独立使用。 |
| `damai-captcha-framework` | Spring Boot 自动装配封装：通过 `AutoConfiguration.imports` 注册 `AjCaptchaAutoConfiguration` 和 `CaptchaAutoConfig`，暴露 `CaptchaController`（`/captcha/get`、`/captcha/check`）、`CaptchaHandle`，并提供 Redis 缓存实现 `CaptchaCacheServiceRedisImpl`，配置项为 `aj.captcha`（`AjCaptchaProperties`）。 |

## 技术栈

- Java 17、Spring Boot 3（`spring-boot-starter-web`、`spring-boot-starter-data-redis`）
- Java AWT（验证码底图绘制）、AES 加密（坐标保护）
- Redis（验证码数据缓存，可选 local 内存缓存）

## 与其他模块的关系

- 依赖项目内模块：`damai-common`（通用工具/返回体，两个子模块均依赖）；`damai-captcha-framework` 依赖 `damai-base-captcha`。
- 被依赖：业务微服务 `damai-user-service`（用户服务）通过引入 `damai-captcha-framework` 实现注册/登录等场景的验证码校验（见 `UserCaptchaController`、`UserRegisterVerifyCaptcha`）。
