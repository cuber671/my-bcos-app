#!/bin/bash

# ============================================================
# ReceivableWithOverdue 智能合约一键部署脚本
# ============================================================

set -e  # 遇到错误立即退出

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目路径
PROJECT_DIR="/home/llm_rca/fisco/my-bcos-app"
CONSOLE_DIR="/home/llm_rca/fisco/console/dist"
CONTRACT_SOURCE="$PROJECT_DIR/contracts/receivable/ReceivableWithOverdue.sol"
CONTRACT_NAME="ReceivableWithOverdue"

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     ReceivableWithOverdue 智能合约一键部署工具                 ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# 步骤 1: 检查依赖
echo -e "${YELLOW}[步骤 1/6] 检查部署环境...${NC}"
if [ ! -f "$CONTRACT_SOURCE" ]; then
    echo -e "${RED}✗ 合约源文件不存在: $CONTRACT_SOURCE${NC}"
    exit 1
fi
echo -e "${GREEN}✓ 合约源文件存在${NC}"

if [ ! -d "$CONSOLE_DIR" ]; then
    echo -e "${RED}✗ FISCO 控制台不存在: $CONSOLE_DIR${NC}"
    exit 1
fi
echo -e "${GREEN}✓ FISCO 控制台存在${NC}"

if [ ! -f "$CONSOLE_DIR/console.sh" ]; then
    echo -e "${RED}✗ 控制台启动脚本不存在${NC}"
    exit 1
fi
echo -e "${GREEN}✓ 控制台脚本存在${NC}"

# 步骤 2: 复制合约到控制台
echo -e "\n${YELLOW}[步骤 2/6] 复制合约到控制台目录...${NC}"
CONTRACT_DEST="$CONSOLE_DIR/contracts/sdk/$CONTRACT_NAME.sol"
mkdir -p "$CONSOLE_DIR/contracts/sdk"
cp "$CONTRACT_SOURCE" "$CONTRACT_DEST"
echo -e "${GREEN}✓ 合约已复制到: $CONTRACT_DEST${NC}"

# 步骤 3: 启动控制台并部署合约
echo -e "\n${YELLOW}[步骤 3/6] 启动控制台并部署合约...${NC}"
cd "$CONSOLE_DIR"

# 创建部署脚本
cat > deploy_temp.txt << EOF
deploy $CONTRACT_NAME
exit
EOF

echo -e "${GREEN}正在部署合约，请稍候...${NC}"

# 执行部署
CONSOLE_OUTPUT=$(bash console.sh deploy_temp.txt 2>&1 || true)
echo "$CONSOLE_OUTPUT"

# 提取合约地址
CONTRACT_ADDRESS=$(echo "$CONSOLE_OUTPUT" | grep -oP 'contract address is \K[0-9a-fA-Fx]{40,42}' | head -1)

if [ -z "$CONTRACT_ADDRESS" ]; then
    echo -e "${RED}✗ 无法提取合约地址，请检查控制台输出${NC}"
    echo -e "${YELLOW}控制台输出:${NC}"
    echo "$CONSOLE_OUTPUT"
    exit 1
fi

# 确保地址以 0x 开头
if [[ ! "$CONTRACT_ADDRESS" =~ ^0x ]]; then
    CONTRACT_ADDRESS="0x$CONTRACT_ADDRESS"
fi

echo -e "${GREEN}✓ 合约部署成功！${NC}"
echo -e "${GREEN}  合约地址: $CONTRACT_ADDRESS${NC}"

# 清理临时文件
rm -f deploy_temp.txt

# 步骤 4: 生成 Java 包装类
echo -e "\n${YELLOW}[步骤 4/6] 生成 Java 包装类...${NC}"
JAVA_OUTPUT_DIR="$PROJECT_DIR/src/main/java/com/fisco/app/contract"
mkdir -p "$JAVA_OUTPUT_DIR"

# 使用 contract2java.sh 生成 Java 类
bash contract2java.sh \
    -org com.fisco.app.contract \
    -d "$JAVA_OUTPUT_DIR" \
    -p "$CONTRACT_NAME" \
    -s "$CONTRACT_DEST" 2>&1 | tee contract2java.log

# 检查生成的文件
GENERATED_JAVA="$JAVA_OUTPUT_DIR/$CONTRACT_NAME.java"
if [ ! -f "$GENERATED_JAVA" ]; then
    echo -e "${RED}✗ Java 包装类生成失败${NC}"
    echo -e "${YELLOW}请检查 contract2java.log${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Java 包装类已生成: $GENERATED_JAVA${NC}"

# 步骤 5: 更新 application.yml
echo -e "\n${YELLOW}[步骤 5/6] 更新应用配置...${NC}"
CONFIG_FILE="$PROJECT_DIR/src/main/resources/application.yml"

# 检查是否已存在配置
if grep -q "receivable-with-overdue:" "$CONFIG_FILE"; then
    echo -e "${YELLOW}配置已存在，更新合约地址...${NC}"
    # 使用 sed 替换地址
    sed -i "s/receivable-with-overdue: \".*\"/receivable-with-overdue: \"$CONTRACT_ADDRESS\"/" "$CONFIG_FILE"
else
    echo -e "${YELLOW}添加新配置...${NC}"
    # 在文件末尾添加配置
    cat >> "$CONFIG_FILE" << EOF

# 智能合约配置
contracts:
  receivable-with-overdue: "$CONTRACT_ADDRESS"
EOF
fi

echo -e "${GREEN}✓ 配置文件已更新${NC}"

# 步骤 6: 取消注释 ContractService 代码
echo -e "\n${YELLOW}[步骤 6/6] 启用智能合约集成...${NC}"
CONTRACT_SERVICE_FILE="$PROJECT_DIR/src/main/java/com/fisco/app/service/ContractService.java"

# 备份文件
if [ ! -f "$CONTRACT_SERVICE_FILE.backup" ]; then
    cp "$CONTRACT_SERVICE_FILE" "$CONTRACT_SERVICE_FILE.backup"
    echo -e "${GREEN}✓ 已备份 ContractService.java${NC}"
fi

# 取消注释合约实例变量
sed -i 's|// private com.fisco.app.contract.ReceivableWithOverdue receivableWithOverdueContract;|private com.fisco.app.contract.ReceivableWithOverdue receivableWithOverdueContract;|g' "$CONTRACT_SERVICE_FILE"

# 取消注释合约加载代码（约 107-114 行）
sed -i '/\/\/ 暂时注释，等待合约部署后取消注释/,/^        }$/ {
    s|^// \(\s*if (receivableWithOverdueContractAddress != null\)|\1|
    s|^// \(\s*log\.info.*ReceivableWithOverdue\)|\1|
    s|^        //$||
}' "$CONTRACT_SERVICE_FILE"

# 取消注释 recordRemind 调用
sed -i '/\/\/ TODO: 取消注释合约部署后/,/String txHash = txReceipt.getTransactionHash();/ {
    s|^// ||
}' "$CONTRACT_SERVICE_FILE"

# 注释掉模拟哈希代码
sed -i 's|^log.warn("ReceivableWithOverdue合约未加载，使用模拟交易哈希");|// \0|' "$CONTRACT_SERVICE_FILE"
sed -i 's|^return "0x" + java.util.UUID.randomUUID().toString().replace("-", "");|// \0|' "$CONTRACT_SERVICE_FILE"

echo -e "${GREEN}✓ ContractService.java 已更新${NC}"

# ============================================================
# 部署完成
# ============================================================
echo -e "\n${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                  部署成功！                                    ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}合约地址: $CONTRACT_ADDRESS${NC}"
echo -e "${GREEN}Java 包装类: $GENERATED_JAVA${NC}"
echo -e "${GREEN}配置文件: $CONFIG_FILE${NC}"
echo ""
echo -e "${YELLOW}后续操作：${NC}"
echo -e "1. 重新编译项目: cd $PROJECT_DIR && mvn clean compile"
echo -e "2. 启动应用: mvn spring-boot:run"
echo -e "3. 测试逾期管理接口"
echo ""
echo -e "${YELLOW}测试命令：${NC}"
echo -e "# 测试催收记录上链"
echo -e "curl -X POST \"http://localhost:8080/api/receivable/{id}/remind\" \\"
echo -e "  -H \"Content-Type: application/json\" \\"
echo -e "  -d '{\"remindType\": \"EMAIL\", \"remindContent\": \"测试\"}'"
echo ""
