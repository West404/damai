# 大麦项目 Docker 部署

## 前置条件

- Docker / Docker Compose v2
- JDK 17 + Maven（仅用于构建 jar，容器内运行不需要）

## 部署步骤

```bash
# 1. 在仓库根目录构建所有模块的 jar
mvn clean package -DskipTests

# 2. 构建镜像并启动全部服务（5 个中间件 + 8 个微服务 + 前端）
docker compose up -d

# 3. 查看启动状态
docker compose ps
docker compose logs -f gateway-service
```

MySQL 首次启动会自动执行 `sql/cloud/` 下的建库建表脚本。
若需重置数据：`docker compose down -v` 后重新 `up -d`。

## 访问入口

| 入口 | 地址 |
|---|---|
| 前端页面 | http://localhost （80 端口，/api 由 Nginx 代理到网关） |
| 网关（前端对接口） | http://localhost:6085 |
| Nacos 控制台 | http://localhost:8848/nacos （nacos/nacos） |
| 服务监控中心 | http://localhost:10082 （admin/admin） |

## 配置说明

- 各微服务通过 compose 中的环境变量覆盖本地配置（Nacos/Redis/Kafka/ES 地址），无需改动 `application.yml`
- user/order/pay/program 四个分库分表服务使用 `docker` profile，加载 `shardingsphere-*-docker.yaml`（MySQL 指向容器 `mysql:3306`）
- JVM 堆内存默认 `-Xms256m -Xmx512m`，可在 compose 各服务的 `environment` 中通过 `JAVA_OPTS` 调整
- 中间件内存：MySQL/ES/Kafka/Nacos 合计约 3~4G，建议宿主机至少 8G 内存
