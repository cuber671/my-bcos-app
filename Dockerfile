# 第一步：构建阶段（适配Maven 3.6.3 + OpenJDK 11）
# 选择和本地Maven/Java匹配的基础镜像：maven:3.6.3-openjdk-11
FROM maven:3.6.3-openjdk-11 AS builder

# 设置工作目录
WORKDIR /app

# 复制pom.xml，利用Maven缓存（pom不变就不重新下载依赖）
COPY pom.xml .

# 下载Maven依赖（适配Maven 3.6.3的命令）
RUN mvn dependency:go-offline -B

# 复制项目源码
COPY src ./src

# 打包项目（跳过测试，测试阶段提速）
RUN mvn clean package -DskipTests

# 第二步：运行阶段（轻量级OpenJDK 11 JRE，减小镜像体积）
FROM openjdk:11-jre-slim

# 设置工作目录
WORKDIR /app

# 从构建阶段复制打包好的jar包
COPY --from=builder /app/target/*.jar app.jar

# 容器启动命令（运行jar包）
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# 暴露Java应用端口（和Compose里的8080对应）
EXPOSE 8080