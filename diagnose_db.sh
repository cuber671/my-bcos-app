#!/bin/bash
# 邀请码数据库诊断脚本

echo "========================================="
echo "  邀请码数据库诊断工具"
echo "========================================="
echo ""

# 1. 检查应用配置的数据库
echo "📌 应用配置的数据库连接信息:"
echo "----------------------------------------"
grep "spring.datasource" /home/llm_rca/fisco/my-bcos-app/src/main/resources/application.properties | grep -v "type\|driver\|druid"
echo ""

# 2. 尝试不同的连接方式
echo "📌 尝试连接数据库..."
echo "----------------------------------------"

# 方式1: 尝试使用fisco_admin
echo "方式1: 使用应用配置的用户名"
mysql -u fisco_admin -p'YourPassword123' bcos_supply_chain -e "SELECT COUNT(*) as count FROM invitation_code;" 2>&1 | grep -v "mysql: \[Warning\]"
echo ""

# 方式2: 尝试使用root
echo "方式2: 使用root用户"
mysql -u root -p'root' bcos_supply_chain -e "SELECT COUNT(*) as count FROM invitation_code;" 2>&1 | grep -v "mysql: \[Warning\]"
echo ""

# 方式3: 不指定密码（可能需要手动输入）
echo "方式3: 无密码连接（可能需要手动输入密码）"
mysql -u root bcos_supply_chain -e "SELECT COUNT(*) as count FROM invitation_code;" 2>&1 | grep -v "mysql: \[Warning\]"
echo ""

# 3. 列出所有数据库
echo "📌 列出所有数据库:"
echo "----------------------------------------"
mysql -u root -p'root' -e "SHOW DATABASES;" 2>&1 | grep -v "mysql: \[Warning\]" | grep -E "bcos|supply|information_schema"
echo ""

# 4. 检查表是否存在
echo "📌 检查invitation_code表是否存在:"
echo "----------------------------------------"
mysql -u root -p'root' -e "SELECT TABLE_NAME, TABLE_ROWS FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'bcos_supply_chain' AND TABLE_NAME LIKE '%invitation%';" 2>&1 | grep -v "mysql: \[Warning\]"
echo ""

# 5. 如果找到表，显示数据
echo "📌 尝试查询邀请码数据:"
echo "----------------------------------------"
mysql -u root -p'root' bcos_supply_chain -e "SELECT id, code, enterprise_id, status, created_at FROM invitation_code ORDER BY id DESC LIMIT 5;" 2>&1 | grep -v "mysql: \[Warning\]"
echo ""

# 6. 检查是否有其他类似的数据库
echo "📌 查找所有包含'bcos'的数据库:"
echo "----------------------------------------"
mysql -u root -p'root' -e "SHOW DATABASES LIKE '%bcos%';" 2>&1 | grep -v "mysql: \[Warning\]"
echo ""

echo "========================================="
echo "  诊断完成"
echo "========================================="
echo ""
echo "💡 提示："
echo "1. 如果方式1-3都失败，请检查MySQL服务是否在运行"
echo "2. 如果找不到表，可能表在不同的数据库中"
echo "3. 如果表存在但没有数据，检查应用程序日志"
echo ""
echo "🔍 通过应用程序验证数据存在性:"
echo "   curl -X GET 'http://localhost:8080/api/invitation-codes/enterprise/0299903e-7d7d-4664-8d5d-c90b51378caa'"
echo ""
