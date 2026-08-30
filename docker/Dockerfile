# 大麦项目微服务通用镜像
# 使用方式：先执行 mvn clean package -DskipTests 构建出各服务 jar，
# 再由 docker-compose.yml 以各服务目录为构建上下文引用本文件
FROM eclipse-temurin:17-jre

WORKDIR /app

# 构建上下文中打包好的服务 jar
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# JVM 参数通过环境变量注入，compose 中统一限制堆内存
ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
