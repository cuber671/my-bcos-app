#!/bin/bash

# 应用启动脚本
# Application Startup Script

# 加载 .env 文件（如果存在）
# 主要是为了获取 JWT_SECRET，数据库配置已在 application.properties 中
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo "Loaded configuration from .env file"
fi

# 设置默认 JWT_SECRET（如果未在.env中设置）
export JWT_SECRET=${JWT_SECRET:-9pwdZFf9c6BZM5F8lLW65xbgcJVfNnAvV9Yd2qzIVEfGFRmtfqXrvE/7BetTt01xfcJ1oWqptwZXXxrALFy5UA==}

echo "======================================"
echo "Starting FISCO BCOS Supply Chain App"
echo "======================================"
echo "Database: fisco_admin@127.0.0.1:3306/bcos_supply_chain"
echo "(已在 application.properties 中配置)"
echo "======================================"

# 启动应用
mvn clean spring-boot:run
