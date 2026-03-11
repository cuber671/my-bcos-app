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

# 创建日志目录
RUN mkdir -p /app/logs

# 2. 从 builder 阶段拷贝 jar 包 (确保 AS builder 定义正确)
COPY --from=builder /app/target/*.jar app.jar


# 4. 复制账户 PEM 文件（SDK 签名使用）
COPY src/main/resources/account /app/resources/account

# 5. 为 SDK 创建账户目录（使用相对路径 account）
RUN mkdir -p /app/account/ecdsa && \
    cp /app/resources/account/*.pem /app/account/ecdsa/ 2>/dev/null || true

# 3. 关键：手动设置 HOME，SDK 释放 .so 文件会用到
ENV HOME=/root

EXPOSE 8080

# 4. 增加启动参数双重保险，解决中文乱码
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dfile.encoding=UTF-8 -Djava.io.tmpdir=/tmp -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar app.jar"]
