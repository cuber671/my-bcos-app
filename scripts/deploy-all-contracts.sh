#!/bin/bash
# =============================================================================
# FISCO BCOS 合约部署脚本
# 用法: ./scripts/deploy-all-contracts.sh
# =============================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
CONSOLE_DIR="$PROJECT_DIR/console"
BUILD_DIR="$CONSOLE_DIR/contracts/build"
ADDRESS_FILE="$PROJECT_DIR/contract-addresses.env"

# 合约部署顺序 (依赖关系)
# 注意: LibAddress 使用 Solidity 0.8 语法，控制台默认 0.5.2 不支持，故跳过
declare -a DEPLOY_ORDER=(
    "lib:LibBytes"
    "lib:LibString"
    "enterprise:EnterpriseRegistryV2"
    "enterprise:EnterpriseRegistryAuth"
    "credit:CreditLimitCore"
    "credit:CreditLimitScore"
    "warehouse:WarehouseReceiptCore"
    "warehouse:WarehouseReceiptOps"
    "warehouse:WarehouseReceiptHistory"
    "receivable:ReceivableCore"
    "receivable:ReceivableRepayment"
    "bill:BillCore"
    "bill:BillOps"
    "logistics:LogisticsCore"
    "logistics:LogisticsOps"
)

# 环境变量名称映射
declare -A ENV_VAR_NAMES=(
    ["lib:LibBytes"]="CONTRACT_LIB_BYTES"
    ["lib:LibString"]="CONTRACT_LIB_STRING"
    ["enterprise:EnterpriseRegistryV2"]="CONTRACT_ENTERPRISE"
    ["enterprise:EnterpriseRegistryAuth"]="CONTRACT_ENTERPRISE_AUTH"
    ["credit:CreditLimitCore"]="CONTRACT_CREDIT_CORE"
    ["credit:CreditLimitScore"]="CONTRACT_CREDIT_SCORE"
    ["warehouse:WarehouseReceiptCore"]="CONTRACT_WAREHOUSE_CORE"
    ["warehouse:WarehouseReceiptOps"]="CONTRACT_WAREHOUSE_OPS"
    ["warehouse:WarehouseReceiptHistory"]="CONTRACT_WAREHOUSE_HISTORY"
    ["receivable:ReceivableCore"]="CONTRACT_RECEIVABLE_CORE"
    ["receivable:ReceivableRepayment"]="CONTRACT_RECEIVABLE_REPAYMENT"
    ["bill:BillCore"]="CONTRACT_BILL_CORE"
    ["bill:BillOps"]="CONTRACT_BILL_OPS"
    ["logistics:LogisticsCore"]="CONTRACT_LOGISTICS_CORE"
    ["logistics:LogisticsOps"]="CONTRACT_LOGISTICS_OPS"
)

echo "=========================================="
echo "FISCO BCOS 合约部署"
echo "=========================================="
echo ""

# 1. 环境检查
echo "[1/4] 检查部署环境..."
echo ""

# 检查 Docker 容器
CONTAINERS_RUNNING=0
for container in fisco-node0 fisco-node1 fisco-node2 fisco-node3 fisco-console; do
    if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        CONTAINERS_RUNNING=$((CONTAINERS_RUNNING + 1))
    fi
done

if [ "$CONTAINERS_RUNNING" -lt 5 ]; then
    echo -e "${RED}错误: 5 个容器必须全部运行，当前仅运行 $CONTAINERS_RUNNING 个${NC}"
    echo "请先启动容器: docker-compose up -d"
    exit 1
fi

# 检查节点连通性
BLOCK_NUMBER=$(curl -s -X POST http://localhost:20000 -H "Content-Type: application/json" \
    -d '{"jsonrpc":"2.0","method":"getBlockNumber","params":[],"id":1}' | grep -o '"result":"0x[^"]*"' | cut -d'"' -f4)

# 如果没有 0x 前缀，尝试直接获取数字
if [ -z "$BLOCK_NUMBER" ]; then
    BLOCK_NUMBER=$(curl -s -X POST http://localhost:20000 -H "Content-Type: application/json" \
        -d '{"jsonrpc":"2.0","method":"getBlockNumber","params":[],"id":1}' | grep -o '"result":[0-9]*' | grep -o '[0-9]*')
fi

if [ -z "$BLOCK_NUMBER" ]; then
    echo -e "${RED}错误: 无法连接到 FISCO 节点${NC}"
    exit 1
fi

# 转换为十进制（处理 0x 前缀）
if [[ "$BLOCK_NUMBER" == 0x* ]]; then
    block_dec=$((16#${BLOCK_NUMBER#0x}))
else
    block_dec=$BLOCK_NUMBER
fi
echo -e "  ✓ 所有容器运行中 (当前块高: $block_dec)"
echo ""

# 2. 准备合约文件
echo "[2/4] 准备合约文件..."
echo ""

# 复制所有合约到控制台 solidity 目录
for item in "${DEPLOY_ORDER[@]}"; do
    module="${item%%:*}"
    contract="${item##*:}"
    contract_file="$PROJECT_DIR/contracts/$module/${contract}.sol"

    if [ -f "$contract_file" ]; then
        echo -n "  复制 $contract ... "
        docker cp "$contract_file" fisco-console:/data/contracts/solidity/ 2>/dev/null || true
        echo -e "${GREEN}✓${NC}"
    fi
done
echo ""

# 3. 部署合约 (控制台直接编译+部署)
echo "[3/4] 部署合约..."
echo ""

TOTAL=${#DEPLOY_ORDER[@]}
SUCCESS=0
FAILED=0

# 存储部署结果
declare -A DEPLOY_ADDRESSES

for item in "${DEPLOY_ORDER[@]}"; do
    module="${item%%:*}"
    contract="${item##*:}"
    index=$((SUCCESS + FAILED + 1))

    echo -n "  [$index/$TOTAL] 部署 $contract ... "

    # 使用控制台直接部署 (控制台自动编译)
    # 输出格式: "contract address: 0x<40位地址>"
    deploy_output=$(echo "deploy $contract" | docker exec -i fisco-console java -cp "apps/*:conf/:lib/*:classes/" console.Console group0 2>&1 || echo "")

    # 提取地址 - 控制台输出 "contract address: 0x..."
    address=$(echo "$deploy_output" | grep -oE '0x[0-9a-fA-F]{40}' | head -1)

    if [ -n "$address" ]; then
        DEPLOY_ADDRESSES[$item]=$address
        echo -e "${GREEN}✓ 地址: $address${NC}"
        SUCCESS=$((SUCCESS + 1))
    else
        echo -e "${RED}✗ (部署失败)${NC}"
        FAILED=$((FAILED + 1))
    fi
done

echo ""
echo "=========================================="
echo "部署完成"
echo "=========================================="
echo "总计: $TOTAL 个合约"
echo "成功: $SUCCESS 个"
echo "失败: $FAILED 个"
echo ""

# 4. 生成地址配置文件
echo "[4/4] 生成地址配置文件..."
echo ""

# 生成 .env 格式的地址文件
{
    echo "# FISCO BCOS 合约地址配置"
    echo "# 生成时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "# 请将此文件内容复制到 .env 文件中"
    echo ""
    for item in "${!DEPLOY_ADDRESSES[@]}"; do
        env_name=${ENV_VAR_NAMES[$item]}
        address=${DEPLOY_ADDRESSES[$item]}
        if [ -n "$env_name" ]; then
            echo "${env_name}=${address}"
        fi
    done
} > "$ADDRESS_FILE"

echo "  地址已保存到: $ADDRESS_FILE"
echo ""

# 输出地址汇总
echo "地址汇总"
echo ""
echo "=========================================="
for item in "${DEPLOY_ORDER[@]}"; do
    env_name=${ENV_VAR_NAMES[$item]}
    address=${DEPLOY_ADDRESSES[$item]}
    contract="${item##*:}"

    if [ -n "$address" ]; then
        echo "${env_name}=${address}"
    fi
done
echo "=========================================="
echo ""
echo -e "${GREEN}部署完成!${NC}"
echo ""
echo "下一步操作:"
echo "  1. 将 $ADDRESS_FILE 中的地址复制到 .env 文件"
echo "  2. 重启应用: docker-compose restart app"
echo ""

# 复制 ABI 文件到应用目录 (供 Java 绑定使用)
mkdir -p "$PROJECT_DIR/src/main/resources/contracts/abi"
cp -f "$BUILD_DIR/abi/"*.abi "$PROJECT_DIR/src/main/resources/contracts/abi/" 2>/dev/null || true

exit 0
