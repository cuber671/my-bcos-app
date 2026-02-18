# ================= 第一阶段：构建应用 =================
# 建议直接指定完整的镜像名，避免 Docker 找不到 library 别名
FROM maven:3.9.6-eclipse-temurin-11 AS builder

WORKDIR /app

# 利用缓存：只在 pom 改变时才下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并打包
COPY src ./src
RUN mvn clean package -DskipTests

# ================= 第二阶段：运行环境 =================
FROM eclipse-temurin:11-jre-focal

# 1. 安装 FISCO BCOS 必须的底层库 libssl-dev
RUN apt-get update && apt-get install -y curl tzdata libssl-dev && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 2. 从 builder 阶段拷贝 jar 包 (确保 AS builder 定义正确)
COPY --from=builder /app/target/*.jar app.jar

# 3. 彻底解决 user dir unavailable：
# 使用 -m 强制创建家目录，设置 HOME 环境变量
RUN groupadd -g 1010 spring && \
    useradd -m -u 1010 -g spring spring && \
    mkdir -p /app/logs /app/sdk && \
    chown -R spring:spring /app

# 关键：手动设置 HOME，SDK 释放 .so 文件会用到
ENV HOME=/home/spring
USER spring

EXPOSE 8080

# 4. 增加启动参数双重保险
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.io.tmpdir=/tmp -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar app.jar"]