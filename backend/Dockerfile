FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package && \
    cp target/work-device-registry-0.1.0.jar /workspace/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
# docker-compose.yml 的 healthcheck 用 curl 探测；temurin:17-jre 默认不含 curl，需显式安装。
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=builder /workspace/app.jar /app/app.jar
ENV TZ=Asia/Shanghai
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
