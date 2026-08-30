# damai-base-captcha

验证码核心基础包（纯 Java，不依赖 Spring），源自开源 AJ-Captcha（anji-plus），提供行为式验证码的核心模型、服务与算法实现。

## 功能说明

- **模型层**：`CaptchaVO`、`PointVO`（坐标）、`CaptchaTypeEnum`（验证码类型：DEFAULT/BLOCK_PUZZLE/CLICK_WORD/ROTATE_PUZZLE）、`ResponseModel`、`RepCodeEnum`（响应码）。
- **服务层**：
  - `CaptchaService`：验证码统一接口（`get` 生成 / `check` 校验 / `verification` 二次验证）；
  - `AbstractCaptchaService`：抽象基类，封装通用流程；
  - 三种验证码实现：`BlockPuzzleCaptchaServiceImpl`（滑动拼图）、`ClickWordCaptchaServiceImpl`（文字点选）、`RotatePuzzleCaptchaServiceImpl`（旋转拼图）；
  - `DefaultCaptchaServiceImpl`、`CaptchaServiceFactory`：按类型创建验证码服务的工厂；
  - `FrequencyLimitHandler`：接口请求频率限制。
- **缓存层**：`CaptchaCacheService` 接口及本地内存实现 `CaptchaCacheServiceMemImpl`（Redis 实现位于 `damai-captcha-framework`）。
- **工具层**：`AesUtil`（坐标 AES 加密）、`ImageUtils`（底图绘制/缓存）、`Md5Util`、`RandomUtils`、`CacheUtil` 等。

## 技术栈

- Java 17、Java AWT（验证码图片绘制）、AES/MD5 加密算法。

## 与其他模块的关系

- 依赖项目内模块：`damai-common`（通用工具/返回体）。
- 被依赖：`damai-captcha-framework`（Spring Boot 自动装配层，间接服务于 `damai-user-service` 等业务服务）。
