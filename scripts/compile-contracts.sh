#!/bin/bash
# =============================================================================
# FISCO BCOS 合约编译脚本
# 用法: ./scripts/compile-contracts.sh
# =============================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
CONSOLE_DIR="$PROJECT_DIR/console"
BUILD_DIR="$CONSOLE_DIR/contracts/build"

echo "=========================================="
echo "FISCO BCOS 合约编译"
echo "=========================================="
echo ""

# 创建构建目录
mkdir -p "$BUILD_DIR/abi"
mkdir -p "$BUILD_DIR/bin"

# 合约模块定义
declare -A CONTRACT_MODULES=(
    ["lib"]="LibBytes,LibString,LibAddress"
    ["enterprise"]="EnterpriseRegistryV2,EnterpriseRegistryAuth"
    ["warehouse"]="WarehouseReceiptCore,WarehouseReceiptOps,WarehouseReceiptHistory,IWarehouseReceiptCore"
    ["receivable"]="ReceivableCore,ReceivableRepayment,IReceivableCore"
    ["credit"]="CreditLimitCore,CreditLimitScore"
    ["bill"]="BillCore,BillOps,IBillCore"
    ["logistics"]="LogisticsCore,LogisticsOps,ILogisticsCore"
)

# 统计
TOTAL=0
SUCCESS=0
FAILED=0

echo "编译合约到: $BUILD_DIR"
echo ""

# 检查 solc 是否可用
if ! docker exec fisco-console which solc > /dev/null 2>&1; then
    echo -e "${YELLOW}控制台中未找到 solc，尝试使用 Docker 外部编译...${NC}"

    # 检查本地 solc
    if ! command -v solc &> /dev/null; then
        echo -e "${RED}错误: 未找到 solc 编译器${NC}"
        echo "请安装 solc: https://docs.soliditylang.org/"
        exit 1
    fi

    # 使用本地 solc 编译
    for module in "${!CONTRACT_MODULES[@]}"; do
        contracts=${CONTRACT_MODULES[$module]}
        IFS=',' read -ra CONTRACT_ARRAY <<< "$contracts"

        for contract in "${CONTRACT_ARRAY[@]}"; do
            contract_file="$PROJECT_DIR/contracts/$module/${contract}.sol"

            if [ -f "$contract_file" ]; then
                echo "编译: $contract ($module)"
                solc --abi --bin --optimize -o "$BUILD_DIR" "$contract_file" 2>/dev/null || true

                if [ -f "$BUILD_DIR/${contract}.abi" ]; then
                    SUCCESS=$((SUCCESS + 1))
                else
                    FAILED=$((FAILED + 1))
                fi
                TOTAL=$((TOTAL + 1))
            fi
        done
    done
else
    # 使用控制台容器内的 solc 编译
    echo "使用控制台容器内 solc 编译..."
    echo ""

    for module in "${!CONTRACT_MODULES[@]}"; do
        contracts=${CONTRACT_MODULES[$module]}
        IFS=',' read -ra CONTRACT_ARRAY <<< "$contracts"

        for contract in "${CONTRACT_ARRAY[@]}"; do
            contract_file="$PROJECT_DIR/contracts/$module/${contract}.sol"

            if [ -f "$contract_file" ]; then
                echo -n "编译: $contract ... "

                # 复制合约到控制台 solidity 目录
                docker cp "$contract_file" fisco-console:/data/contracts/solidity/

                # 在控制台内编译
                if docker exec fisco-console bash -c "solc --abi --bin --optimize -o /data/contracts/build /data/contracts/solidity/${contract}.sol 2>/dev/null"; then
                    # 复制回宿主机
                    docker cp "fisco-console:/data/contracts/build/${contract}.abi" "$BUILD_DIR/abi/" 2>/dev/null || true
                    docker cp "fisco-console:/data/contracts/build/${contract}.bin" "$BUILD_DIR/bin/" 2>/dev/null || true

                    if [ -f "$BUILD_DIR/abi/${contract}.abi" ]; then
                        echo -e "${GREEN}✓${NC}"
                        SUCCESS=$((SUCCESS + 1))
                    else
                        echo -e "${RED}✗ (ABI 未生成)${NC}"
                        FAILED=$((FAILED + 1))
                    fi
                else
                    echo -e "${RED}✗${NC}"
                    FAILED=$((FAILED + 1))
                fi
                TOTAL=$((TOTAL + 1))
            fi
        done
    done
fi

echo ""
echo "=========================================="
echo "编译完成"
echo "=========================================="
echo "总计: $TOTAL 个合约"
echo "成功: $SUCCESS 个"
echo "失败: $FAILED 个"
echo ""

# 列出生成的文件
echo "生成的文件:"
ls -la "$BUILD_DIR/abi/" 2>/dev/null | head -20
ls -la "$BUILD_DIR/bin/" 2>/dev/null | head -20
echo ""

if [ "$FAILED" -gt 0 ]; then
    echo -e "${RED}警告: 有 $FAILED 个合约编译失败${NC}"
    exit 1
else
    echo -e "${GREEN}✓ 所有合约编译成功${NC}"
    exit 0
fi
