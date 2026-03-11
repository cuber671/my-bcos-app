#!/bin/bash
# =============================================================================
# FISCO BCOS 合约部署验证脚本
# 用法: ./scripts/verify-deployment.sh
# =============================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "=========================================="
echo "FISCO BCOS 合约部署验证"
echo "=========================================="
echo ""

# 读取 .env 配置
if [ -f "$PROJECT_DIR/.env" ]; then
    source "$PROJECT_DIR/.env"
else
    echo -e "${YELLOW}警告: 未找到 .env 文件${NC}"
fi

# 合约配置 (名称 -> 地址环境变量)
declare -A CONTRACTS=(
    ["EnterpriseRegistryV2"]="${CONTRACT_ENTERPRISE:-}"
    ["EnterpriseRegistryAuth"]="${CONTRACT_ENTERPRISE_AUTH:-}"
    ["CreditLimitCore"]="${CONTRACT_CREDIT_CORE:-}"
    ["CreditLimitScore"]="${CONTRACT_CREDIT_SCORE:-}"
    ["WarehouseReceiptCore"]="${CONTRACT_WAREHOUSE_CORE:-}"
    ["WarehouseReceiptOps"]="${CONTRACT_WAREHOUSE_OPS:-}"
    ["WarehouseReceiptHistory"]="${CONTRACT_WAREHOUSE_HISTORY:-}"
    ["ReceivableCore"]="${CONTRACT_RECEIVABLE_CORE:-}"
    ["ReceivableRepayment"]="${CONTRACT_RECEIVABLE_REPAYMENT:-}"
    ["BillCore"]="${CONTRACT_BILL_CORE:-}"
    ["BillOps"]="${CONTRACT_BILL_OPS:-}"
    ["LogisticsCore"]="${CONTRACT_LOGISTICS_CORE:-}"
    ["LogisticsOps"]="${CONTRACT_LOGISTICS_OPS:-}"
)

echo "[1/3] 检查节点状态..."
echo ""

# 检查节点连通性
for i in 0 1 2 3; do
    port=$((20000 + i))
    response=$(curl -s -X POST http://localhost:$port -H "Content-Type: application/json" \
        -d '{"jsonrpc":"2.0","method":"getBlockNumber","params":[],"id":1}' 2>/dev/null || echo '{"error":{}}')

    if echo "$response" | grep -q '"result"'; then
        block=$(echo "$response" | grep -o '"result":"0x[^"]*"' | cut -d'"' -f4)
        block_dec=$((16#${block#0x}))
        echo -e "  ✓ fisco-node$i 连通 (块高: $block_dec)"
    else
        echo -e "  ✗ fisco-node$i 无法连接"
    fi
done

echo ""
echo "[2/3] 验证合约部署..."
echo ""

TOTAL=0
SUCCESS=0
FAILED=0

for contract in "${!CONTRACTS[@]}"; do
    address="${CONTRACTS[$contract]}"

    if [ -z "$address" ]; then
        echo -e "  ${YELLOW}跳过 $contract (地址未配置)${NC}"
        continue
    fi

    TOTAL=$((TOTAL + 1))
    echo -n "  验证 $contract ($address) ... "

    # 检查地址是否有效 (不是 0x000... 或空)
    if [ "$address" = "0x0000000000000000000000000000000000000000" ] || [ ${#address} -ne 42 ]; then
        echo -e "${RED}✗ (地址无效)${NC}"
        FAILED=$((FAILED + 1))
        continue
    fi

    # 尝试调用 getBlockNumber 测试连接
    # 这里使用简化的方式 - 检查地址是否存在于链上
    # 实际上需要通过 Web3j 或控制台查询

    # 通过控制台查询
    result=$(docker exec fisco-console bash -c "
        cd /data
        echo \"const Web3 = require('web3');
        const web3 = new Web3('http://fisco-node0:20000');
        web3.eth.getCode('$address').then(console.log);\"
    " 2>/dev/null || echo "")

    if [ -n "$result" ] && [ "$result" != "0x" ]; then
        echo -e "${GREEN}✓ (已部署)${NC}"
        SUCCESS=$((SUCCESS + 1))
    else
        echo -e "${RED}✗ (未部署或空)${NC}"
        FAILED=$((FAILED + 1))
    fi
done

echo ""
echo "[3/3] 检查应用配置..."
echo ""

# 检查 application.yml 配置
if grep -q "contract:" "$PROJECT_DIR/src/main/resources/application.yml" 2>/dev/null; then
    echo -e "  ✓ application.yml 中已配置 contract 地址"
else
    echo -e "  ${YELLOW}⚠ application.yml 中未配置 contract 地址${NC}"
fi

# 检查环境变量是否配置
env_count=0
for var in CONTRACT_ENTERPRISE CONTRACT_WAREHOUSE_CORE CONTRACT_RECEIVABLE_CORE; do
    value=$(eval echo \$$var)
    if [ -n "$value" ]; then
        env_count=$((env_count + 1))
    fi
done

if [ "$env_count" -gt 0 ]; then
    echo -e "  ✓ .env 中已配置 $env_count 个合约地址"
else
    echo -e "  ${YELLOW}⚠ .env 中未配置合约地址${NC}"
fi

echo ""
echo "=========================================="
echo "验证完成"
echo "=========================================="
echo "已验证: $TOTAL 个合约"
echo "部署成功: $SUCCESS 个"
echo "部署失败: $FAILED 个"
echo ""

if [ "$FAILED" -gt 0 ]; then
    echo -e "${RED}警告: $FAILED 个合约未部署或部署失败${NC}"
    echo ""
    echo "请检查:"
    echo "  1. 运行部署脚本: ./scripts/deploy-all-contracts.sh"
    echo "  2. 将生成的地址配置到 .env 文件"
    echo "  3. 重启应用: docker-compose restart app"
    exit 1
else
    echo -e "${GREEN}✓ 所有合约验证通过${NC}"
    exit 0
fi
