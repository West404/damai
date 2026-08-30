# damai-elasticsearch-framework

Elasticsearch 封装模块（单一 jar 模块，非父模块），为业务服务提供开箱即用的 ES 低层级客户端封装，统一处理索引管理、文档写入与多条件检索（含地理位置、分页、排序、高亮），避免各业务服务直接拼装原生 ES DSL。

## 功能说明

基于 Spring Boot 自动装配机制，引入本模块并配置 `elasticsearch.ip` 后，即可注入 `BusinessEsHandle` 直接使用，无需手动创建 ES 客户端。

核心类：

- `com.damai.conf.BusinessEsAutoConfig`：自动装配类（通过 `META-INF/spring/...AutoConfiguration.imports` 注册）。在配置存在 `elasticsearch.ip` 时生效，创建 `RestClient`（支持多节点、用户名/密码认证、超时与连接数配置）并注册 `BusinessEsHandle` Bean。
- `com.damai.conf.BusinessEsProperties`：配置属性类，前缀 `elasticsearch`，包含 `ip`、`userName`、`passWord`、`esSwitch`（总开关）、`esTypeSwitch`（兼容带 type 的旧版本 ES）、各类超时与连接数参数。
- `com.damai.util.BusinessEsHandle`：ES 操作核心工具类，提供：
  - 索引管理：`createIndex`（按 mapping 参数建索引）、`checkIndex`、`deleteIndex`、`deleteData`；
  - 文档写入/删除：`add`（可指定文档 id）、`deleteByDocumentId`；
  - 检索：多组 `query` / `queryPage` 重载，支持精确/分词查询、时间范围查询、普通字段排序、geo 距离查询与排序、`search_after` 深分页、结果高亮，分页结果封装为 `PageInfo`。
- DTO 参数对象：`EsDataQueryDto`（查询条件）、`EsDocumentMappingDto`（字段映射）、`EsGeoPointDto` / `EsGeoPointSortDto`（经纬度查询/排序）、`EsDataCreateDto`。

## 技术栈

依据本模块 `pom.xml` 的实际依赖：

- Spring Boot `spring-boot-starter-web`（排除自带日志）
- Spring Data Elasticsearch `4.0.9.RELEASE`（使用底层 `RestClient` 直连 REST API，排除 transport 客户端）
- PageHelper（仅使用其 `PageInfo` 作为分页返回体，已排除 mybatis 等无关依赖）
- 项目内模块 `damai-common`
- 间接使用：Hutool、Fastjson、Lombok、Apache HttpClient（ES RestClient 底层）

## 与其他模块的关系

- **依赖的项目内模块**：`damai-common`（通用工具类，如 `StringUtil`）。
- **被依赖方**：`damai-server/damai-program-service`（节目服务），用于节目搜索场景——`ProgramEs` 检索、`ProgramElasticsearchInitData` 启动时初始化 ES 数据、`ProgramShowTimeRenewal` 演出时间更新时同步 ES。
- 本模块为独立 starter 式封装，其他业务服务如需 ES 能力，在 pom 中引入本模块并配置 `elasticsearch.*` 即可。
