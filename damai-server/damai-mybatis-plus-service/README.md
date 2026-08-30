# damai-mybatis-plus-service

## 功能说明

本模块是 MyBatis-Plus 的**代码生成器工具模块**，它不是一个需要启动部署的微服务，没有启动类、配置类和 Controller。

模块中只有一个核心类：

- `com.damai.MybatisPlusGenerator`：基于 MyBatis-Plus 的 `FastAutoGenerator` 实现的代码生成器，包含一个 `main` 方法，直接以 Java 程序方式运行（注释中明确说明"并不需要启动"）。主要能力包括：
  - 连接 MySQL 数据库，根据指定表（如 `d_user_mobile`）生成对应的 Entity、Mapper、Service 等代码；
  - 自定义类型转换（将 `SMALLINT`、`TINYINT` 映射为 Java `Integer`）；
  - 使用 Freemarker 模板引擎（默认是 Velocity）渲染生成代码；
  - 支持过滤表前缀（`d_`）、指定输出目录和 Mapper XML 生成路径。

它的定位是开发辅助工具：当需要为新表生成 MyBatis-Plus 的 service、do、实体映射等代码时，可参考此类的配置运行生成。

## 技术栈

- **MyBatis-Plus**（`mybatis-plus-spring-boot3-starter`）：ORM 框架
- **MyBatis-Plus Generator**（`mybatis-plus-generator`）：代码生成器
- **Freemarker**：代码生成模板引擎
- **MySQL Connector/J**：数据库驱动
- **Jasypt**（`jasypt-spring-boot-starter`）：配置加密

## 与其他模块的关系

- **依赖的项目内模块**：无。本模块只依赖第三方库（见 `pom.xml`），不依赖 `damai-common` 等项目内任何模块。
- **被哪些模块依赖**：无。本模块仅作为 `damai-server` 聚合 pom 中的一个 module 参与构建，其他业务微服务均未在 pom 中引用它（生成的代码才是被各业务服务使用的产物）。

## 使用方式

直接运行 `MybatisPlusGenerator` 的 `main` 方法即可生成代码，运行前请按需修改其中的数据库连接地址、表名（`addInclude`）、表前缀和输出目录。
