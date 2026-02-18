#!/bin/bash

# ============================================
# ReceivableWithOverdue 合约部署脚本
# FISCO BCOS v3
# ============================================

set -e  # 遇到错误立即退出

# 配置变量
CONTRACT_NAME="ReceivableWithOverdue"
CONTRACT_FILE="${CONTRACT_NAME}.sol"
OUTPUT_DIR="./build/contracts"

# FISCO BCOS 控制台路径（根据实际路径修改）
FISCO_CLI="/fisco/bcos-console/console.py"

# 链配置
CHANNEL_ID=1
GROUP_ID=1

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  ReceivableWithOverdue 合约部署脚本${NC}"
echo -e "${GREEN}============================================${NC}"

# 检查合约文件是否存在
if [ ! -f "contracts/${CONTRACT_FILE}" ]; then
    echo -e "${RED}错误: 合约文件不存在: contracts/${CONTRACT_FILE}${NC}"
    exit 1
fi

echo -e "${YELLOW}步骤 1: 编译智能合约...${NC}"
# 使用 solc 编译合约
solc --bin --abi --optimize -o ${OUTPUT_DIR} \
    contracts/${CONTRACT_FILE}

echo -e "${GREEN}✓ 合约编译完成${NC}"

# 获取合约地址（从控制台或配置文件）
BIN_FILE="${OUTPUT_DIR}/${CONTRACT_NAME}.bin"
ABI_FILE="${OUTPUT_DIR}/${CONTRACT_NAME}.abi"

if [ ! -f "$BIN_FILE" ]; then
    echo -e "${RED}错误: 编译失败，未找到bin文件${NC}"
    exit 1
fi

echo -e "${YELLOW}步骤 2: 准备部署...${NC}"
echo "合约二进制: $BIN_FILE"
echo "合约ABI: $ABI_FILE"

echo -e "${YELLOW}步骤 3: 部署合约到 FISCO BCOS...${NC}"
echo -e "${YELLOW}请选择部署方式:${NC}"
echo "1. 使用 FISCO 控制台部署"
echo "2. 使用 SDK 部署"
echo "3. 使用 Web3JS/Geth 部署"
read -p "请输入选项 (1-3): " deploy_option

case $deploy_option in
    1)
        echo -e "${YELLOW}使用 FISCO 控制台部署...${NC}"
        echo "请在控制台中执行以下命令："
        echo ""
        echo "deploy ${CONTRACT_NAME}"
        echo ""
        echo -e "${YELLOW}部署完成后，将返回的合约地址更新到 application.yml${NC}"
        ;;
    2)
        echo -e "${YELLOW}使用 SDK 部署（需要实现 Java 部署代码）...${NC}"
        echo "请运行应用程序中的部署接口或测试类"
        ;;
    3)
        echo -e "${YELLOW}使用 Web3JS 部署...${NC}"
        echo "请使用 Web3JS 脚本或工具部署"
        ;;
    *)
        echo -e "${RED}无效选项${NC}"
        exit 1
        ;;
esac

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  部署脚本执行完成${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "${YELLOW}下一步操作:${NC}"
echo "1. 将合约地址更新到 application.yml:"
echo "   contracts:"
echo "     receivable-with-overdue: ${NC}\"0xYourContractAddressHere\"${NC}"
echo ""
echo "2. 取消注释 ContractService 中的合约调用代码"
echo "3. 重启应用程序"
echo ""
